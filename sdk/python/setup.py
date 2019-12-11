#!/usr/bin/env python3
from setuptools import setup
from datetime import datetime

# See https://packaging.python.org/tutorials/packaging-projects/
# for details about packaging python projects

# Generating distribution archives (run from same directory as this file)
# python3 -m pip install --user --upgrade setuptools wheel
# python3 setup.py sdist bdist_wheel


# External pip dependencies are loaded from the requirements-zsdk.txt file. If any
# additional pip installs are required add them to that file.
with open('requirements.txt') as f:
    reqs = f.read().strip().splitlines()

setup(
    name='pixml',
    version=open("VERSION").read().strip(),
    description='PixelML Python SDK',
    url='https://www.zorroa.com',
    license='Copyright ' + str(datetime.now().year) + ' Zorroa Corp. All Rights Reserved.',
    package_dir={'': 'pylib'},
    packages=['pixml', 'pixml.analysis'],
    scripts=[],

    classifiers=[
        "Programming Language :: Python :: 3",
        # "License :: OSI Approved :: MIT License",  # TODO Add license type
        "Operating System :: OS Independent",        # TODO confirm this
    ],

    include_package_data=True,
    install_requires=reqs
)
