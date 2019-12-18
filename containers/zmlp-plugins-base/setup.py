#!/usr/bin/env python3
from setuptools import setup
from datetime import datetime

with open('requirements.txt') as f:
    reqs = f.read().strip().splitlines()

setup(
    name='pixmld',
    version="1.0.0",
    description='PixML container daemon',
    url='https://www.pixml.com"',
    license='Copyright ' + str(datetime.now().year) + ' Zorroa Corp. All Rights Reserved.',
    package_dir={'': 'pylib'},
    packages=['containerizer'],
    scripts=['server'],

    classifiers=[
        "Programming Language :: Python :: 3",
        # "License :: OSI Approved :: MIT License",  # TODO Add license type
        "Operating System :: OS Independent",        # TODO confirm this
    ],

    include_package_data=True,
    install_requires=reqs
)
