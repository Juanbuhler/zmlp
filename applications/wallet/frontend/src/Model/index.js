import Head from 'next/head'
import { useRouter } from 'next/router'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'
import Tabs from '../Tabs'
import ModelLabels from '../ModelLabels'

import ModelDetails from './Details'

const Model = () => {
  const { pathname } = useRouter()

  return (
    <>
      <Head>
        <title>Model Details</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Custom Models', href: '/[projectId]/models' },
          { title: 'Model Details', href: false },
        ]}
      />

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <ModelDetails key={pathname} />

        <Tabs
          tabs={[
            { title: 'View Labels', href: '/[projectId]/models/[modelId]' },
          ]}
        />

        <ModelLabels />
      </SuspenseBoundary>
    </>
  )
}

export default Model
