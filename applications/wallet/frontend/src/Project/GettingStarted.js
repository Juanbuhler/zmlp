import PropTypes from 'prop-types'
import Link from 'next/link'

import { typography, colors, spacing, constants } from '../Styles'

import Card, { VARIANTS as CARD_VARIANTS } from '../Card'
import Button, { VARIANTS } from '../Button'

import DataSourcesSvg from '../Icons/datasources.svg'
import JobQueueSvg from '../Icons/jobQueue.svg'
import VisualizerSvg from '../Icons/visualizer.svg'

const STEPS = [
  {
    step: 1,
    title: 'Create a Data Source',
    module: (
      <>
        <DataSourcesSvg height={constants.iconSize} />
        Data Sources
      </>
    ),
    content:
      'Data Sources define the source location of files and the types of ' +
      'Machine Learning analysis to be performed on them. Source files can be ' +
      'located in any of the major cloud provider object stores (GCS, S3, Azure Blob).',
    cta: '+ Create Data Source',
    link: '/[projectId]/data-sources/add',
  },
  {
    step: 2,
    title: 'Review Job Progress',
    module: (
      <>
        <JobQueueSvg height={constants.iconSize} />
        Job Queue
      </>
    ),
    content:
      'After a Data Source is created a processing job is launched that performs ' +
      'the Machine Learning analysis and stores the resulting metadata. The Job Queue ' +
      'is where you will monitor the progress of these jobs and manage them.',
    cta: 'Go To Job Queue',
    link: '/[projectId]/jobs',
  },
  {
    step: 3,
    title: 'Inspect Your Dataset',
    module: (
      <>
        <VisualizerSvg height={constants.iconSize} />
        Visualizer
      </>
    ),
    content:
      'Once the processing job has successfully completed you can view the results in the ' +
      'Visualizer. The Visualizer is a unified visual tool for inspecting and tuning ' +
      'the metadata output of the Machine Learning analysis.',
    cta: 'Go To Visualizer',
    link: '/[projectId]/visualizer',
  },
]

const ProjectGettingStarted = ({ projectId }) => {
  return (
    <Card
      variant={CARD_VARIANTS.LIGHT}
      header="Getting Started"
      content={STEPS.map(({ step, title, module, content, cta, link }) => (
        <div
          key={step}
          css={{ paddingBottom: step === STEPS.length ? 0 : spacing.comfy }}
        >
          <div
            css={{
              borderBottom:
                step === STEPS.length ? 0 : constants.borders.regular.iron,
            }}
          >
            <h4
              css={{
                fontWeight: typography.weight.regular,
                fontSize: typography.size.medium,
                lineHeight: typography.height.medium,
                color: colors.key.one,
              }}
            >
              <span css={{ fontWeight: typography.weight.bold }}>
                Step {step}:&nbsp;
              </span>
              {title}
            </h4>
            <h5
              css={{
                fontSize: typography.size.regular,
                lineHeight: typography.height.regular,
                fontWeight: typography.weight.bold,
                margin: 0,
                paddingTop: spacing.normal,
                display: 'flex',
                alignItems: 'center',
                svg: { marginRight: spacing.base },
              }}
            >
              {module}
            </h5>
            <p
              css={{
                margin: 0,
                paddingTop: spacing.normal,
                paddingBottom: spacing.normal,
                color: colors.structure.zinc,
              }}
            >
              {content}
            </p>
            <div
              css={{
                display: 'flex',
                paddingBottom: step === STEPS.length ? 0 : spacing.comfy,
              }}
            >
              <Link
                href={link}
                as={link.replace('[projectId]', projectId)}
                passHref
              >
                <Button variant={VARIANTS.SECONDARY_SMALL}>{cta}</Button>
              </Link>
            </div>
          </div>
        </div>
      ))}
    />
  )
}

ProjectGettingStarted.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default ProjectGettingStarted
