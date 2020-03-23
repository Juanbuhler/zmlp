import { useRef, useEffect } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'

import { spacing, colors, constants, typography } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Button, { VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import SectionTitle from '../SectionTitle'

import { onCopy } from '../Copy/helpers'

const ApiKeysAddFormSuccess = ({
  projectId,
  apikey: { permissions },
  apikey,
  onReset,
}) => {
  const copyRef = useRef()

  useEffect(() => {
    onCopy({ copyRef })
  }, [])

  return (
    <div>
      <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
        Key Generated &amp; Copied to Clipboard
      </FlashMessage>

      <SectionTitle>Scope</SectionTitle>

      <ul css={{ color: colors.structure.zinc }}>
        {permissions.map((permission) => (
          <li key={permission}>
            {permission.replace(/([A-Z])/g, (match) => ` ${match}`)}
          </li>
        ))}
      </ul>

      <SectionTitle>API Key</SectionTitle>

      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          paddingTop: spacing.normal,
          paddingBottom: spacing.normal,
        }}
      >
        <textarea
          ref={copyRef}
          defaultValue={JSON.stringify(apikey)}
          rows="5"
          css={{
            width: constants.form.maxWidth,
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            color: colors.structure.white,
            backgroundColor: colors.structure.mattGrey,
            borderRadius: constants.borderRadius.small,
            padding: spacing.base,
            border: 'none',
            resize: 'none',
          }}
        />
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            paddingLeft: spacing.small,
            paddingRight: spacing.small,
          }}
        >
          <Button
            variant={VARIANTS.LINK}
            style={{
              '&:active': {
                opacity: 0,
                transition: 'opacity .3s ease',
              },
            }}
            onClick={() => onCopy({ copyRef })}
          >
            Copy Key
          </Button>
          <span css={{ padding: spacing.small, color: colors.structure.steel }}>
            |
          </span>
          <Button
            variant={VARIANTS.LINK}
            download="api-key.json"
            href={`data:application/octet-stream;charset=utf-8;base64,${window.btoa(
              JSON.stringify(apikey),
            )}`}
          >
            Download
          </Button>
        </div>
      </div>

      <ButtonGroup>
        <Button variant={VARIANTS.SECONDARY} onClick={onReset}>
          Create Another Key
        </Button>
        <Link
          href="/[projectId]/api-keys"
          as={'/[projectId]/api-keys'.replace('[projectId]', projectId)}
          passHref
        >
          <Button variant={VARIANTS.PRIMARY}>View All</Button>
        </Link>
      </ButtonGroup>
    </div>
  )
}

ApiKeysAddFormSuccess.propTypes = {
  projectId: PropTypes.string.isRequired,
  apikey: PropTypes.shape({
    permissions: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
    secretKey: PropTypes.string.isRequired,
  }).isRequired,
  onReset: PropTypes.func.isRequired,
}

export default ApiKeysAddFormSuccess
