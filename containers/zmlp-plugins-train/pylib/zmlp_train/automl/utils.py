# import logging
# import tempfile
#
# from google.cloud import automl
#
# from zmlpsdk import file_storage
#
# logger = logging.getLogger(__name__)
#
#
# def _import_images_into_dataset(self, dataset):
#     """
#     Import images to the AutoML dataset.
#
#     Args:
#         dataset (DataSet): The automl dataset.
#
#     """
#     self.emit_status(f'Importing labeled images into {self.display_name}')
#
#     labels_url = self._store_labels_file()
#
#     gcs_source = automl.types.GcsSource(input_uris=[labels_url])
#     input_config = automl.types.InputConfig(gcs_source=gcs_source)
#     result = self.client.import_data(dataset.name, input_config).result()
#
#     logger.info("Processing import...")
#     logger.info("Data imported. {}".format(result))
#
# def _store_labels_file(self):
#     """
#     Write a DataSet in a AutoML training structure.
#
#     Returns:
#         str: A path to data file.
#     """
#     self.emit_status(f'Building labels file for {self.display_name}')
#
#     csv_file = tempfile.mkstemp(".csv")
#     query = self.model.get_label_search()
#
#     with open(csv_file, "w") as fp:
#         for asset in self.app.assets.scroll_search(query, timeout='5m'):
#             label = self._get_label(asset)
#             if not label:
#                 continue
#
#             tag = label.get('label')
#             scope = label.get('scope')
#
#             test = ""
#             if scope == "TEST":
#                 test = "TEST"
#
#             # get proxy uri
#             proxy_uri = self._get_img_proxy_uri(asset)
#             if proxy_uri:
#                 fp.write(f"{test}{proxy_uri},{tag}\n")
#
#     ref = file_storage.projects.store_file(
#         csv_file, self.model, "automl", "labels.csv")
#
#     return file_storage.projects.get_native_uri(ref)
#
# def _get_img_proxy_uri(self, asset):
#     """
#     Get a URI to the img proxy
#
#     Args:
#         asset: (Asset): The asset to find an audio proxy for.
#
#     Returns:
#         str: A URI to the smallest image proxy if not empty else empty string
#     """
#     img_proxies = asset.get_files(
#         mimetype="image/",
#         category='proxy',
#         sort_func=lambda f: f.attrs.get('width', 0)
#     )
#
#     if img_proxies:
#         img_proxy = img_proxies[0]  # get the smallest proxy
#         return file_storage.assets.get_native_uri(img_proxy)
#     return None
#
# def _get_label(self, asset):
#     """
#     Get the current model label for the given asset.
#
#     Args:
#         asset (Asset): The asset to check.
#
#     Returns:
#         list[dict]: The labels for training a model.
#
#     """
#     ds_labels = asset.get_attr('labels')
#     if not ds_labels:
#         return None
#
#     for ds_label in ds_labels:
#         if ds_label.get('modelId') == self.model.id:
#             return ds_label
#     return None
#
# def _create_automl_session(self, dataset, op_name):
#     """
#     Registers the dataset and training job op with the
#     Archivist.
#
#     Args:
#         dataset (DataSet): The AutoML Dataset
#         op_name (str): The training job op.
#
#     Returns:
#
#     """
#     self.emit_status(f'Registering training op {op_name}')
#     body = {
#         "automlDataSet": dataset.name,
#         "automlTrainingJob": op_name
#     }
#     return self.app.client.post(f'/api/v1/models/{self.model.id}/_automl', body)
