import io
from collections import namedtuple

from ..asset import Asset, StoredFile
from ..job import Job
from ..search import AssetSearchResult, AssetSearchScroller, SimilarityQuery
from ..util import as_collection


class AssetApp(object):

    def __init__(self, app):
        self.app = app

    def batch_import_files(self, assets, modules=None):
        """
        Import a list of FileImport instances.

        Args:
            assets (list of FileImport): The list of files to import as Assets.
            modules (list): A list of Pipeline Modules to apply to the data.

        Notes:
            Example return value:
                {
                  "bulkResponse" : {
                    "took" : 15,
                    "errors" : false,
                    "items" : [ {
                      "create" : {
                        "_index" : "yvqg1901zmu5bw9q",
                        "_type" : "_doc",
                        "_id" : "dd0KZtqyec48n1q1fniqVMV5yllhRRGx",
                        "_version" : 1,
                        "result" : "created",
                        "forced_refresh" : true,
                        "_shards" : {
                          "total" : 1,
                          "successful" : 1,
                          "failed" : 0
                        },
                        "_seq_no" : 0,
                        "_primary_term" : 1,
                        "status" : 201
                      }
                    } ]
                  },
                  "failed" : [ ],
                  "created" : [ "dd0KZtqyec48n1q1fniqVMV5yllhRRGx" ],
                  "jobId" : "ba310246-1f87-1ece-b67c-be3f79a80d11"
                }

        Returns:
            dict: A dictionary containing an ES bulk response, failed files,
            and created asset ids.

        """
        body = {
            "assets": assets,
            "modules": modules
        }
        return self.app.client.post("/api/v3/assets/_batch_create", body)

    def batch_upload_files(self, assets, modules=None):
        """
        Batch upload a list of files and return a structure which contains
        an ES bulk response object, a list of failed file paths, a list of created
        asset Ids, and a processing jobId.

        Args:
            assets (list of FileUpload):
            modules (list): A list of Pipeline Modules to apply to the data.

        Notes:
            Example return value:
                {
                  "bulkResponse" : {
                    "took" : 15,
                    "errors" : false,
                    "items" : [ {
                      "create" : {
                        "_index" : "yvqg1901zmu5bw9q",
                        "_type" : "_doc",
                        "_id" : "dd0KZtqyec48n1q1fniqVMV5yllhRRGx",
                        "_version" : 1,
                        "result" : "created",
                        "forced_refresh" : true,
                        "_shards" : {
                          "total" : 1,
                          "successful" : 1,
                          "failed" : 0
                        },
                        "_seq_no" : 0,
                        "_primary_term" : 1,
                        "status" : 201
                      }
                    } ]
                  },
                  "failed" : [ ],
                  "created" : [ "dd0KZtqyec48n1q1fniqVMV5yllhRRGx" ],
                  "jobId" : "ba310246-1f87-1ece-b67c-be3f79a80d11"
                }

        Returns:
            dict: A dictionary containing an ES bulk response, failed files,
            and created asset ids.
        """
        assets = as_collection(assets)
        files = [asset.uri for asset in assets]
        body = {
            "assets": assets,
            "modules": modules
        }
        return self.app.client.upload_files("/api/v3/assets/_batch_upload",
                                            files, body)

    def index(self, asset):
        """
        Re-index an existing asset.  The metadata for the entire asset
        is overwritten by the local copy.

        Args:
            asset (Asset): The asset

        Notes:
            Example return value:
                {
                  "_index" : "v4mtygyqqpsjlcnv",
                  "_type" : "_doc",
                  "_id" : "dd0KZtqyec48n1q1fniqVMV5yllhRRGx",
                  "_version" : 2,
                  "result" : "updated",
                  "_shards" : {
                    "total" : 1,
                    "successful" : 1,
                    "failed" : 0
                  },
                  "_seq_no" : 1,
                  "_primary_term" : 1
                }

        Examples:
            asset = app.assets.get_asset(id)
            asset.set_attr("aux.my_field", 1000)
            asset.remove_attr("aux.other_field")
            app.assets.index(asset)

        Returns:
            dict: An ES update response.
        """
        return self.app.client.put("/api/v3/assets/{}/_index".format(asset.id),
                                   asset.document)

    def update(self, asset, doc):
        """
        Update a given Asset with a partial document dictionary.

        Args:
            asset: (mixed): An Asset object or unique asset id.
            doc: (dict): the changes to apply.

        Notes:
            Doc argument example:
                {
                    "aux": {
                        "captain": "kirk"
                    }
                }

            Example return value:
                {
                  "_index" : "9l0l2skwmuesufff",
                  "_type" : "_doc",
                  "_id" : "dd0KZtqyec48n1q1fniqVMV5yllhRRGx",
                  "_version" : 2,
                  "result" : "updated",
                  "_shards" : {
                    "total" : 1,
                    "successful" : 1,
                    "failed" : 0
                  },
                  "_seq_no" : 1,
                  "_primary_term" : 1
                }

        Returns
            dict: The ES update response object.
        """
        asset_id = getattr(asset, "id", None) or asset
        body = {
            "doc": doc
        }
        return self.app.client.post("/api/v3/assets/{}/_update".format(asset_id), body)

    def batch_index(self, assets):
        """
        Reindex multiple existing assets.  The metadata for the entire asset
        is overwritten by the local copy.

        Notes:
            Example return value:
                {
                  "took" : 11,
                  "errors" : false,
                  "items" : [ {
                    "index" : {
                      "_index" : "qjdjbpkvwg0sgusl",
                      "_type" : "_doc",
                      "_id" : "dd0KZtqyec48n1q1fniqVMV5yllhRRGx",
                      "_version" : 2,
                      "result" : "updated",
                      "_shards" : {
                        "total" : 1,
                        "successful" : 1,
                        "failed" : 0
                      },
                      "_seq_no" : 1,
                      "_primary_term" : 1,
                      "status" : 200
                    }
                  } ]
                }

        Returns:
            dict: An ES BulkResponse object.

        """
        body = dict([(a.id, a.document) for a in assets])
        return self.app.client.post("/api/v3/assets/_batch_index", body)

    def batch_update(self, docs):
        """
        Args:
            docs (dict): A dictionary of asset Id to document.

        Notes:
            Example request dictionary
                {
                    "assetId1": {
                        "doc": {
                            "aux": {
                                "captain": "kirk"
                            }
                        }
                    },
                    "assetId2": {
                        "doc": {
                            "aux": {
                                "captain": "kirk"
                            }
                        }
                    }
                }

        Returns:
            dict: An ES BulkResponse object.

        """
        return self.app.client.post("/api/v3/assets/_batch_update", docs)

    def delete(self, asset):
        """
        Delete the given asset.

        Args:
            asset (mixed): unique Id or Asset instance.

        Returns:
            An ES Delete response.

        """
        asset_id = getattr(asset, "id", None) or asset
        return self.app.client.delete("/api/v3/assets/{}".format(asset_id))

    def delete_by_query(self, search):
        """
        Delete assets by the given search.

        Args:
            search (dict): An ES search.

        Notes:
            Example Request:
                {
                    "query": {
                        "terms": {
                            "source.filename": {
                                "bob.jpg"
                            }
                        }
                    }
                }

        Returns:
            An ES delete by query response.

        """
        return self.app.client.delete("/api/v3/assets/_delete_by_query", search)

    def search(self, search=None):
        """
        Perform an asset search using the ElasticSearch query DSL.

        See Also:
            For search/query format.
            https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html

        Args:
            search (dict): The ElasticSearch search to execute.
        Returns:
            AssetSearchResult - an AssetSearchResult instance.
        """
        return AssetSearchResult(self.app, search)

    def scroll_search(self, search=None, timeout="1m"):
        """
        Perform an asset scrolled search using the ElasticSearch query DSL.

        See Also:
            For search/query format.
            https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html

        Args:
            search (dict): The ElasticSearch search to execute
            timeout (str): The scroll timeout.
        Returns:
            AssetSearchScroll - an AssetSearch instance.
        """
        return AssetSearchScroller(self.app, search, timeout)

    def reprocess_search(self, search, modules):
        """
        Reprocess the given search with the supplied modules.

        Args:
            search (dict): An ElasticSearch search.
            modules (list): A list of module names to apply.

        Returns:
            dict: Contains a Job and the number of assets to be processed.
        """
        body = {
            "search": search,
            "modules": modules
        }
        rsp = self.app.client.post("/api/v3/assets/_search/reprocess", body)
        return ReprocessSearchResponse(rsp["assetCount"], Job(rsp["job"]))

    def reprocess_assets(self, assets, modules):
        """
        Reprocess the given array of assets with the given modules.

        Args:
            assets (list): A list of Assets or asset unique Ids.
            modules (list): A list of Pipeline module names or ides.

        Returns:
            Job: The job responsible for processing the assets.
        """
        asset_ids = [getattr(asset, "id") or asset for asset in as_collection(assets)]
        body = {
            "search": {
                "query": {
                    "terms": {
                        "_id": asset_ids
                    }
                }
            },
            "modules": modules
        }
        return self.app.client.get("/api/v3/assets/_search/reprocess", body)

    def get_asset(self, id):
        """
        Return the asset with the given unique Id.

        Args:
            id (str): The unique ID of the asset.

        Returns:
            Asset: The Asset
        """
        return Asset(self.app.client.get("/api/v3/assets/{}".format(id)))

    def download_file(self, stored_file):
        """
        Download given file and store results in memory.  The stored_file ID
        can be specified as either a string like "assets/<id>/proxy/image_450x360.jpg"
        or a StoredFile instance can be used.

        Args:
            stored_file (mixed): The StoredFile instance or its ID.

        Returns:
            io.BytesIO instance containing the binary data.

        """
        if isinstance(stored_file, str):
            path = stored_file
        elif isinstance(stored_file, StoredFile):
            path = stored_file.id
        else:
            raise ValueError("stored_file must be a string or StoredFile instance")

        rsp = self.app.client.get("/api/v3/files/_stream/{}".format(path), is_json=False)
        return io.BytesIO(rsp.content)

    def download_file_to_file(self, stored_file, dst_path):
        """
        Download given file and store it in th destination path.
        The stored_file ID can be specified as either a string like
        "assets/<id>/proxy/image_450x360.jpg" or a StoredFile
        instance like those from Asset.get_files can be used.

        Args:
            stored_file (mixed): The StoredFile instance or its ID.
            dst_path (str): The path to the destination file.

        Returns:
            int: the file size in bytes
        """
        if isinstance(stored_file, str):
            path = stored_file
        elif isinstance(stored_file, StoredFile):
            path = stored_file.id
        else:
            raise ValueError("stored_file must be a string or StoredFile instance")

        rsp = self.app.client.get("/api/v3/files/_stream/{}".format(path), is_json=False)
        with open(dst_path, 'wb') as fp:
            fp.write(rsp.content)
        return len(rsp.content)

    def get_sim_hashes(self, images):
        """
        Return a similarity hash for the given array of images.

        Args:
            images (mixed): Can be an file handle (opened with 'rb'), or
                path to a file.
        Returns:
            list of str: A list of similarity hashes.

        """
        return self.app.client.upload_files("/ml/v1/sim-hash",
                                            as_collection(images), body=None)

    def get_sim_query(self, images, min_score=0.75):
        """
        Analyze the given image files and return a SimilarityQuery which
        can be used in a search.

        Args:
            images (mixed): Can be an file handle (opened with 'rb'), or
                path to a file.
            min_score (float): A float between, the higher the value the more similar
                the results.  Defaults to 0.75

        Returns:
            SimilarityQuery: A configured SimilarityQuery
        """
        return SimilarityQuery(self.get_sim_hashes(images), min_score)


"""
A named tuple to define a ReprocessSearchResponse
"""
ReprocessSearchResponse = namedtuple('ReprocessSearchResponse', ["asset_count", "job"])
