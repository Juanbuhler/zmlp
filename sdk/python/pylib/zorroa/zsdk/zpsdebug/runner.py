import docker
import json

from zorroa.zsdk.testing import TestEventEmitter
from zorroa.zsdk.zps.process import ProcessorExecutor
from zorroa.zsdk.processor import Reactor


class ZpsRunner(object):

    def __init__(self, processor, args, image, data):
        self.processor = processor
        self.args = args
        self.image = image
        self.data = data

    def run(self):
        if self.image is not None:
            self.run_in_container()
        else:
            self.run_locally()

    def run_locally(self):
        emitter = TestEventEmitter()
        pe = ProcessorExecutor(Reactor(emitter))

        req = {
            "ref": {
                "className": self.processor,
                "args": self.args
            },
            "object": self.data
        }
        pe.execute_processor(req)

    def run_in_container(self):
        client = docker.from_env()
        volumes = {'/tmp': {'bind': '/tmp', 'mode': "rw"}}

        cmd = [
            self.processor
        ]

        if self.args:
            cmd.extend(("--args", json.dumps(self.args)))

        if self.data:
            cmd.extend(("--data", json.dumps(self.data)))

        res = client.containers.run(self.image, cmd,
                                    stderr=True,
                                    stream=True,
                                    entrypoint="/usr/local/bin/zpsdebug",
                                    volumes=volumes)
        for line in res:
            print(line)
