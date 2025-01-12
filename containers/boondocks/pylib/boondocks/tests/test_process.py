import unittest
from unittest.mock import patch

from boondocks.logs import setup_logging
from boondocks.process import ProcessorExecutor, AssetConsumer, is_file_type_allowed
from boondocks.reactor import Reactor
from boonflow import Frame
from boonflow.testing import TestEventEmitter, TestAsset, TestProcessor

setup_logging()

TEST_IMAGE = "boonai/plugins-base:latest"


class ProcessorExecutorTests(unittest.TestCase):

    def setUp(self):
        self.emitter = TestEventEmitter()
        self.pe = ProcessorExecutor(Reactor(self.emitter))

    def test_get_processor_key(self):
        ref1 = {
            "className": "foo.bar",
            "args": {"pop": "tarts"}
        }
        ref2 = {
            "className": "foo.bar",
            "args": {"pop": "tart"}
        }
        key1 = self.pe.get_processor_key(ref1)
        key2 = self.pe.get_processor_key(ref2)
        assert key1 != key2

    def test_execute_processor(self):
        req = {
            "ref": {
                "className": "boonflow.testing.TestProcessor",
                "args": {},
                "image": TEST_IMAGE
            },
            "assets": [
                {"id": "1234", "document": {}}
            ]
        }
        assets = self.pe.execute_processor(req)
        assert self.emitter.event_count("asset") == 1
        assert self.emitter.event_count("error") == 0
        assert self.emitter.event_total() == 1

        # Make sure we got metrics
        assert assets[0]["document"]["metrics"]["pipeline"]
        assert "boonflow.testing.TestProcessor" \
               == assets[0]["document"]["metrics"]["pipeline"][0]["processor"]

    def test_execute_processor_and_raise(self):
        req = {
            "ref": {
                "className": "boonflow.testing.TestProcessor",
                "args": {"raise": True},
                "image": TEST_IMAGE
            },
            "assets": [
                {
                    "id": "1234",
                    "document": {
                        "source": {
                            "path": "/foo/bing.jpg"
                        }
                    }
                }
            ]
        }

        asset = self.pe.execute_processor(req)[0]
        assert self.emitter.event_count("asset") == 1
        assert self.emitter.event_count("error") == 1
        assert self.emitter.event_total() == 2

        error = self.emitter.get_events("error")[0]
        assert error["payload"]["processor"] == "boonflow.testing.TestProcessor"
        assert error["payload"]["fatal"] is False
        assert error["payload"]["phase"] == "execute"
        assert error["payload"]["path"] == "/foo/bing.jpg"
        assert asset["document"]["metrics"]["pipeline"][0]["error"] == "error"

    def test_execute_preprocess(self):
        req = {
            "ref": {
                "className": "boonflow.testing.TestProcessor",
                "image": TEST_IMAGE
            },
            "assets": [
                {
                    "id": "1234",
                    "document": {
                        "source": {
                            "path": "/foo/bing.jpg"
                        }
                    }
                }
            ]
        }
        self.pe.execute_preprocess(req)
        assert self.pe.get_processor_wrapper(req["ref"], {}).instance.preprocess_ran

    def test_execute_processor_and_raise_fatal(self):
        req = {
            "ref": {
                "className": "boonflow.testing.TestProcessor",
                "args": {"raise_fatal": True},
                "image": TEST_IMAGE
            },
            "assets": [
                {
                    "id": "1234",
                    "document": {
                        "source": {
                            "path": "/foo/bing.jpg"
                        }
                    }
                }
            ]
        }

        asset = self.pe.execute_processor(req)[0]
        assert self.emitter.event_count("asset") == 1
        assert self.emitter.event_count("error") == 1
        assert self.emitter.event_total() == 2

        error = self.emitter.get_events("error")[0]
        assert error["payload"]["processor"] == "boonflow.testing.TestProcessor"
        assert error["payload"]["fatal"] is True
        assert error["payload"]["phase"] == "execute"
        assert error["payload"]["path"] == "/foo/bing.jpg"
        assert asset["document"]["metrics"]["pipeline"][0]["error"] == "fatal"

    def test_execute_processor_with_fatal_errors_setting(self):
        req = {
            "ref": {
                "className": "boonflow.testing.TestProcessor",
                "args": {"raise": True},
                "image": TEST_IMAGE
            },
            "assets": [
                {
                    "id": "1234",
                    "document": {
                        "source": {
                            "path": "/foo/bing.jpg"
                        }
                    }
                }
            ]
        }

        # All errors are fatal
        TestProcessor.fatal_errors = True

        try:
            self.pe.execute_processor(req)[0]
            assert self.emitter.event_count("asset") == 1
            assert self.emitter.event_count("error") == 1
            assert self.emitter.event_total() == 2

            asset = self.emitter.get_events("asset")[0]
            assert asset['payload']['skip'] is True

            error = self.emitter.get_events("error")[0]
            assert error["payload"]["processor"] == "boonflow.testing.TestProcessor"
            assert error["payload"]["fatal"] is True
            assert error["payload"]["phase"] == "execute"
            assert error["payload"]["path"] == "/foo/bing.jpg"
        finally:
            TestProcessor.fatal_errors = False

    def test_apply_metrics(self):
        ref = {
            "className": "boonflow.testing.TestProcessor",
            "args": {},
            "image": TEST_IMAGE,
            "module": "standard"
        }
        frame = Frame(TestAsset())
        wrapper = self.pe.get_processor_wrapper(ref, {})
        wrapper.apply_metrics(frame.asset, True, 10, None)

        metrics = frame.asset["metrics"]["pipeline"][0]
        assert "boonflow.testing.TestProcessor" == metrics['processor']
        assert "standard" == metrics["module"]
        assert 10 == metrics["executionTime"]
        assert None is not metrics["executionDate"]
        assert "standard" in frame.asset.get_attr("tmp.produced_analysis")

    def test_apply_metrics_process_false(self):
        ref = {
            "className": "boonflow.testing.TestProcessor",
            "args": {},
            "image": TEST_IMAGE
        }
        frame = Frame(TestAsset())
        wrapper = self.pe.get_processor_wrapper(ref, {})
        wrapper.apply_metrics(frame.asset, True, 10, None)
        wrapper.apply_metrics(frame.asset, False, 10, None)
        metrics = frame.asset["metrics"]["pipeline"][0]
        assert 10 == metrics["executionTime"]

    @patch.object(Reactor, 'check_expand')
    def test_teardown_processor(self, react_patch):
        req = {
            "ref": {
                "className": "boonflow.testing.TestProcessor",
                "args": {},
                "image": TEST_IMAGE
            },
            "assets": [
                {
                    "id": "1234"
                }
            ]
        }
        self.pe.execute_processor(req)
        self.pe.teardown_processor(req)
        stats = self.emitter.get_events("stats")
        assert len(stats) == 1

        react_patch.assert_called_with(force=True)

    def test_get_processor_wrapper(self):
        ref = {
            "className": "boonflow.testing.TestProcessor",
            "args": {},
            "image": TEST_IMAGE
        }
        wrapper = self.pe.get_processor_wrapper(ref, {})
        assert wrapper is not None
        assert wrapper.instance is not None
        assert wrapper.ref == ref

        wrapper2 = self.pe.get_processor_wrapper(ref, {})
        assert wrapper2.ref == wrapper.ref

    def test_new_processor_instance(self):
        ref = {
            "className": "boonflow.testing.TestProcessor",
            "args": {},
            "image": TEST_IMAGE,
        }
        instance = self.pe.new_processor_instance(ref)
        assert instance.__class__.__name__ == "TestProcessor"

    def test_new_processor_init_failure(self):
        ref = {
            "className": "boonflow.testing.TestProcessor",
            "args": {"raise_on_init": True},
            "image": TEST_IMAGE,
        }
        wrapper = self.pe.get_processor_wrapper(ref, {})
        assert wrapper.instance is None
        errors = self.emitter.get_events("error")
        assert len(errors) == 1

    def test_is_aleady_processed(self):
        ref = {
            "className": "boonflow.testing.TestProcessor",
            "args": {},
            "image": TEST_IMAGE,
            "checksum": 122
        }
        frame = Frame(TestAsset())
        wrapper = self.pe.get_processor_wrapper(ref, {})

        assert not wrapper.is_already_processed(frame.asset)
        wrapper.apply_metrics(frame.asset, True, 10, None)
        assert wrapper.is_already_processed(frame.asset)

        # Now override with _force=true
        ref["force"] = True
        assert not wrapper.is_already_processed(frame.asset)

    def test_is_aleady_processed_checksum_check(self):
        ref = {
            "className": "boonflow.testing.TestProcessor",
            "args": {},
            "image": TEST_IMAGE,
            "checksum": 122
        }
        frame = Frame(TestAsset())
        wrapper = self.pe.get_processor_wrapper(ref, {})

        assert not wrapper.is_already_processed(frame.asset)
        wrapper.apply_metrics(frame.asset, True, 10, None)
        assert wrapper.is_already_processed(frame.asset)

        frame.asset["metrics"]["pipeline"][0]["checksum"] = 500
        assert not wrapper.is_already_processed(frame.asset)

    def test_is_aleady_processed_error_check(self):
        ref = {
            "className": "boonflow.testing.TestProcessor",
            "args": {},
            "image": TEST_IMAGE,
            "checksum": 122
        }
        frame = Frame(TestAsset())
        wrapper = self.pe.get_processor_wrapper(ref, {})

        assert not wrapper.is_already_processed(frame.asset)
        wrapper.apply_metrics(frame.asset, True, 10, "warning")
        # errors are always not processed.
        assert not wrapper.is_already_processed(frame.asset)


class TestAssetConsumer(unittest.TestCase):

    def setUp(self):
        self.emitter = TestEventEmitter()
        self.reactor = Reactor(self.emitter)
        self.consumer = AssetConsumer(self.reactor, {"fileTypes": ["jpg", "mp4"]})

    def testAccept(self):
        asset1 = TestAsset("gs://foo/bar/bing.jpg")
        assert self.consumer.accept(asset1)

        asset2 = TestAsset("gs://foo/bar/car.exr")
        assert not self.consumer.accept(asset2)

    def testExpand(self):
        self.consumer.batch_size = 2
        asset1 = TestAsset("gs://foo/bar/bing.jpg")
        assert self.consumer.accept(asset1)
        assert len(self.consumer.expand) == 1
        assert self.consumer.accept(asset1)
        assert len(self.consumer.expand) == 0


class TypeFilterTests(unittest.TestCase):

    def test_is_file_type_allowed(self):
        asset = TestAsset("gs://foo/bar/bing.jpg")
        assert is_file_type_allowed(asset, frozenset(["jpg"]))
        assert not is_file_type_allowed(asset, frozenset(["png"]))
