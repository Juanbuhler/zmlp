# Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
# PDX-License-Identifier: MIT-0 (For details,
# see https://github.com/awsdocs/amazon-rekognition-developer-guide/blob/master/LICENSE-SAMPLECODE.)

import os
import tempfile
import logging

from zmlp_analysis.utils.prechecks import Prechecks
from zmlp_analysis.aws.util import AwsEnv
from zmlp_analysis.aws.videos import util
from zmlpsdk import AssetProcessor, Argument, FileTypes, ZmlpEnv, file_storage, proxy, clips, video

logger = logging.getLogger(__name__)


class AbstractVideoDetectProcessor(AssetProcessor):
    """ AWS Rekognition for Video Detection"""
    namespace = 'aws-video-detection'
    file_types = FileTypes.videos

    def __init__(self, extract_type=None, reactor=None):
        super(AbstractVideoDetectProcessor, self).__init__()
        self.add_arg(Argument('debug', 'bool', default=False))
        self.extract_type = extract_type
        self.reactor = reactor

        self.rek_client = None
        self.s3_client = None
        self.sqs_client = None
        self.sns_client = None

        self.jobId = None
        self.roleArn = None
        self.bucket = None
        self.video = None
        self.startJobId = None

        self.sqsQueueUrl = None
        self.snsTopicArn = None
        self.processType = None

    def init(self):
        self.rek_client = AwsEnv.rekognition()
        self.s3_client = AwsEnv.s3()
        self.sqs_client = AwsEnv.general_aws_client(service='sqs')
        self.sns_client = AwsEnv.general_aws_client(service='sns')
        self.roleArn = AwsEnv.get_rekognition_role_arn()

        self.jobId = ''
        self.bucket = ''
        self.video = ''
        self.startJobId = ''

        self.sqsQueueUrl = ''
        self.snsTopicArn = ''
        self.processType = ''

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        final_time = asset.get_attr('media.length')

        if not Prechecks.is_valid_video_length(asset):
            return

        video_proxy = proxy.get_video_proxy(asset)

        if not video_proxy:
            self.logger.warning(f'No video could be found for {asset_id}')
            return

        local_path = file_storage.localize_file(video_proxy)
        clip_tracker = clips.ClipTracker(asset, self.namespace)

        ext = os.path.splitext(local_path)[1]
        bucket_file = f'{ZmlpEnv.get_project_id()}/video/{asset_id}{ext}'
        bucket_name = AwsEnv.get_bucket_name()

        # upload to s3
        self.s3_client.upload_file(local_path, bucket_name, bucket_file)

        sns_topic_arn, sqs_queue_url = self.create_topic_queue(asset_id)
        try:
            clip_tracker = self.start_detection_analysis(
                clip_tracker=clip_tracker,
                local_video_path=local_path,
                role_arn=self.roleArn,
                bucket=bucket_name,
                video=bucket_file,
                sns_topic_arn=sns_topic_arn,
                sqs_queue_url=sqs_queue_url
            )
        finally:
            self.sqs_client.delete_queue(QueueUrl=sqs_queue_url)
            self.sns_client.delete_topic(TopicArn=sns_topic_arn)

        timeline = clip_tracker.build_timeline(final_time)
        video.save_timeline(timeline)

    def create_topic_queue(self, name):
        """
        Create AWS SNS Topic and SQS Queue

        Args:
            name: (str) the name that will be prepended to the topic and queue

        Returns:
            tuple(str, str) (SNS Topic ARN, SQS Queue URL)
        """
        prepend_name = "AmazonRekognition"

        return util.create_topic_and_queue(
            self.sns_client,
            self.sqs_client,
            topic_name=f"{prepend_name}-{name}-topic",
            queue_name=f"{prepend_name}-{name}-queue"
        )

    def start_detection_analysis(self, clip_tracker, local_video_path, role_arn, bucket, video,
                                 sns_topic_arn, sqs_queue_url):
        """
        Start Detection Analysis

        Args:
            role_arn: (str) AWS Role ARN
            bucket: (str) Bucket name only (i.e. "zorroa-dev" in "gs://zorroa-dev")
            video: (str) video name without extension ("video" instead of "video.mp4")
            sns_topic_arn: (str) SNS Topic ARN
            sqs_queue_url: (str) SQS Queue URL

        Returns:
            (dict) Label Detection Results
        """
        raise NotImplementedError

    def get_detection_results(self, clip_tracker, rek_client, start_job_id, local_video_path,
                              max_results=10):
        """
        Get detection results

        Args:
            clip_tracker: ClipTracker for building Timeline
            rek_client: AWS Rekog Client
            start_job_id: (str) Job ID
            local_video_path: (str) locally created video file
            max_results: (int) maximum results to get, default 10

        Returns:
            (ClipTracker) built clip tracker clips for timeline building
        """
        raise NotImplementedError


class LabelVideoDetectProcessor(AbstractVideoDetectProcessor):
    """ Label Detection for Videos using AWS """
    def __init__(self):
        super(LabelVideoDetectProcessor, self).__init__()

    def start_detection_analysis(self, clip_tracker, local_video_path, role_arn, bucket, video,
                                 sns_topic_arn, sqs_queue_url):
        """
        Start Label Detection Analysis

        Args:
            role_arn: (str) AWS Role ARN
            bucket: (str) Bucket name only (i.e. "zorroa-dev" in "gs://zorroa-dev")
            video: (str) video name without extension ("video" instead of "video.mp4")
            sns_topic_arn: (str) SNS Topic ARN
            sqs_queue_url: (str) SQS Queue URL

        Returns:
            (dict) Label Detection Results
        """
        start_job_id = util.start_label_detection(
            self.rek_client,
            bucket=bucket,
            video=video,
            role_arn=role_arn,
            sns_topic_arn=sns_topic_arn
        )
        if util.get_sqs_message_success(self.sqs_client, sqs_queue_url, start_job_id):
            clip_tracker = self.get_detection_results(
                clip_tracker=clip_tracker,
                rek_client=self.rek_client,
                start_job_id=start_job_id,
                local_video_path=local_video_path,
            )

        return clip_tracker

    def get_detection_results(self, clip_tracker, rek_client, start_job_id, local_video_path,
                              max_results=10):
        """
        Get detection results

        Args:
            clip_tracker: ClipTracker for building Timeline
            rek_client: AWS Rekog Client
            start_job_id: (str) Job ID
            local_video_path: (str) locally created video file
            max_results: (int) maximum results to get, default 10

        Returns:
            (ClipTracker) built clip tracker clips for timeline building
        """
        pagination_token = ''
        finished = False

        output_path = tempfile.mkstemp(".jpg")[1]
        while not finished:
            response = rek_client.get_label_detection(JobId=start_job_id,
                                                      MaxResults=max_results,
                                                      NextToken=pagination_token,
                                                      SortBy='TIMESTAMP')
            for labelDetection in response['Labels']:
                label = labelDetection['Label']
                name = label['Name']
                confidence = label['Confidence']
                start_time = labelDetection['Timestamp'] / 1000  # ms to s

                logger.debug(f'\tLabel: {name}')
                logger.debug(f'\tConfidence: {confidence}')

                video.extract_thumbnail_from_video(local_video_path, output_path,
                                                   start_time)
                clip_tracker.append(start_time, [name])

            if 'NextToken' in response:
                pagination_token = response['NextToken']
            else:
                finished = True

        return clip_tracker


class SegmentVideoDetectProcessor(AbstractVideoDetectProcessor):
    """ Segment Detection for Videos using AWS """
    def __init__(self, cue=None):
        super(SegmentVideoDetectProcessor, self).__init__()
        self.cue = cue

    def start_detection_analysis(self, clip_tracker, local_video_path, role_arn, bucket, video,
                                 sns_topic_arn, sqs_queue_url):
        """
        Start Segment Detection Analysis

        Args:
            role_arn: (str) AWS Role ARN
            bucket: (str) Bucket name only (i.e. "zorroa-dev" in "gs://zorroa-dev")
            video: (str) video name without extension ("video" instead of "video.mp4")
            sns_topic_arn: (str) SNS Topic ARN
            sqs_queue_url: (str) SQS Queue URL

        Returns:
            (dict) Label Detection Results
        """
        start_job_id = util.start_segment_detection(
            self.rek_client,
            bucket=bucket,
            video=video,
            role_arn=role_arn,
            sns_topic_arn=sns_topic_arn
        )
        if util.get_sqs_message_success(self.sqs_client, sqs_queue_url, start_job_id):
            clip_tracker = self.get_detection_results(
                clip_tracker=clip_tracker,
                rek_client=self.rek_client,
                start_job_id=start_job_id,
                local_video_path=local_video_path
            )

        return clip_tracker

    def get_detection_results(self, clip_tracker, rek_client, start_job_id, local_video_path,
                              max_results=10):
        """
        Get detection results

        Args:
            clip_tracker: ClipTracker for building Timeline
            rek_client: AWS Rekog Client
            start_job_id: (str) Job ID
            local_video_path: (str) locally created video file
            max_results: (int) maximum results to get, default 10

        Returns:
            (ClipTracker) built clip tracker clips for timeline building
        """
        pagination_token = ''
        finished = False

        output_path = tempfile.mkstemp(".jpg")[1]
        while not finished:
            response = rek_client.get_segment_detection(JobId=start_job_id,
                                                        MaxResults=max_results,
                                                        NextToken=pagination_token)
            for segment in response['Segments']:
                if segment['Type'] == 'TECHNICAL_CUE':
                    segment_type = segment['TechnicalCueSegment']['Type']
                    if segment_type == self.cue:
                        confidence = segment['TechnicalCueSegment']['Confidence']
                        start_time = segment['StartTimestampMillis'] / 1000  # ms to s

                        logger.debug('Technical Cue')
                        logger.debug(f'\tConfidence: {confidence}')
                        logger.debug(f'\tType: {segment_type}')

                        video.extract_thumbnail_from_video(local_video_path, output_path,
                                                           start_time)
                        clip_tracker.append(start_time, [segment_type])

            if 'NextToken' in response:
                pagination_token = response['NextToken']
            else:
                finished = True

        return clip_tracker


class BlackFramesVideoDetectProcessor(SegmentVideoDetectProcessor):
    """ Black Frames Detector in a video using AWS Rekognition """
    def __init__(self):
        super(BlackFramesVideoDetectProcessor, self).__init__(cue='BlackFrames')


class EndCreditsVideoDetectProcessor(SegmentVideoDetectProcessor):
    """ Rolling Credits Detector in a video using AWS Rekognition """

    def __init__(self):
        super(EndCreditsVideoDetectProcessor, self).__init__(cue='EndCredits')
