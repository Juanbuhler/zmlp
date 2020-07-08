import PropTypes from 'prop-types'
import Link from 'next/link'

import userShape from '../User/shape'

import { colors, typography, spacing, constants } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'

import { openContactForm } from '../Zendesk/helpers'

const SIZE = 28
const ICON_SIZE = 20

const UserMenu = ({ user, user: { firstName, lastName, email }, logout }) => {
  return (
    <div css={{ marginRight: -spacing.moderate }}>
      <Menu
        open="left"
        button={({ onBlur, onClick, isMenuOpen }) => (
          <Button
            aria-label="Open user menu"
            variant={VARIANTS.MENU}
            style={{
              ...(isMenuOpen
                ? { backgroundColor: colors.structure.smoke }
                : {}),
              ':hover': {
                cursor: 'pointer',
              },
            }}
            onBlur={onBlur}
            onClick={onClick}
            isDisabled={false}
          >
            <div css={{ display: 'flex', alignItems: 'center' }}>
              <div
                css={{
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center',
                  border: 0,
                  margin: 0,
                  padding: 0,
                  width: SIZE,
                  height: SIZE,
                  borderRadius: SIZE,
                  color: isMenuOpen
                    ? colors.structure.white
                    : colors.structure.lead,
                  backgroundColor: isMenuOpen
                    ? colors.structure.lead
                    : colors.structure.steel,
                  fontWeight: typography.weight.bold,
                }}
              >
                {`${firstName ? firstName[0] : ''}${
                  lastName ? lastName[0] : ''
                }`}
              </div>
              <ChevronSvg
                height={ICON_SIZE}
                color={colors.structure.steel}
                css={{
                  marginLeft: spacing.base,
                  transform: `${isMenuOpen ? 'rotate(-180deg)' : ''}`,
                }}
              />
            </div>
          </Button>
        )}
      >
        {({ onBlur, onClick }) => (
          <div>
            <div
              css={{
                padding: spacing.normal,
                borderBottom: constants.borders.regular.zinc,
              }}
            >
              <div css={{ fontWeight: typography.weight.bold }}>
                {`${firstName} ${lastName}`}
              </div>
              <div>{email}</div>
            </div>
            <ul css={{ borderBottom: constants.borders.regular.zinc }}>
              <li>
                <Link href="/account" passHref>
                  <Button
                    variant={VARIANTS.MENU_ITEM}
                    onBlur={onBlur}
                    onClick={onClick}
                    isDisabled={false}
                  >
                    Manage Account
                  </Button>
                </Link>
              </li>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={(event) => {
                    onClick(event)

                    openContactForm({ user })
                  }}
                  isDisabled={false}
                >
                  Contact Support
                </Button>
              </li>
            </ul>
            <ul>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={() => logout({ redirectUrl: '/' })}
                  isDisabled={false}
                >
                  Sign Out
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>
    </div>
  )
}

UserMenu.propTypes = {
  user: PropTypes.shape(userShape).isRequired,
  logout: PropTypes.func.isRequired,
}

export default UserMenu
