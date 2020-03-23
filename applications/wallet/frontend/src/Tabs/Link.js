import PropTypes from 'prop-types'
import Link from 'next/link'
import { useRouter } from 'next/router'

import { colors, spacing, typography } from '../Styles'

const TABS_UNDERLINE_WIDTH = 1
const TAB_BOTTOM_WIDTH = 2

const TabsLink = ({ title, href }) => {
  const { pathname, query } = useRouter()

  const isCurrentPage = pathname === href

  return (
    <li
      css={{
        paddingRight: spacing.normal,
        marginBottom: -TABS_UNDERLINE_WIDTH,
      }}
    >
      <Link
        href={href}
        as={href
          .split('/')
          .map((s) => s.replace(/\[(.*)\]/gi, (_, group) => query[group]))
          .join('/')}
        passHref
      >
        <a
          css={{
            border: `0 ${colors.key.one} solid`,
            borderBottomWidth: isCurrentPage ? TAB_BOTTOM_WIDTH : 0,
            color: isCurrentPage ? colors.white : colors.structure.zinc,
            display: 'flex',
            alignItems: 'center',
            paddingTop: spacing.base,
            paddingBottom: spacing.base,
            textTransform: 'uppercase',
            fontSize: typography.size.medium,
            lineHeight: typography.height.medium,
            fontWeight: typography.weight.bold,
            ':hover': {
              textDecoration: 'none',
              color: isCurrentPage ? colors.white : colors.key.one,
            },
          }}
        >
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
