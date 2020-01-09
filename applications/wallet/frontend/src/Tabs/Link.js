import PropTypes from 'prop-types'
import Link from 'next/link'
import { useRouter } from 'next/router'

import { colors, spacing, typography } from '../Styles'

const TabsLink = ({ title, href }) => {
  const {
    pathname,
    query: { projectId },
  } = useRouter()

  const isCurrentPage = pathname === `/[projectId]${href}`

  return (
    <li css={{ paddingRight: spacing.normal }}>
      <Link href={`/[projectId]${href}`} as={`/${projectId}${href}`} passHref>
        <a
          css={{
            border: `0 ${colors.key.two} solid`,
            borderBottomWidth: isCurrentPage ? 2 : 0,
            color: isCurrentPage ? colors.white : colors.structure.zinc,
            display: 'flex',
            alignItems: 'center',
            paddingTop: spacing.base,
            paddingBottom: spacing.base,
            textTransform: 'uppercase',
            fontSize: typography.size.kilo,
            lineHeight: typography.height.kilo,
            fontWeight: typography.weight.bold,
            ':hover': {
              textDecoration: 'none',
              color: isCurrentPage ? colors.white : colors.key.one,
            },
          }}>
          {title}
        </a>
      </Link>
    </li>
  )
}

TabsLink.propTypes = {
  title: PropTypes.string.isRequired,
  href: PropTypes.string.isRequired,
}

export default TabsLink
