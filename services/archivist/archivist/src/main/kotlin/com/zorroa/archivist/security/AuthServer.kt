package com.zorroa.archivist.security

import com.zorroa.auth.client.ZmlpActor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.UUID



object KnownKeys {

    /**
     * The job runner key has the correct perms to run a job.
     */
    val JOB_RUNNER = "job-runner"

    /**
     * The background-thread key doesn't actually exist but is an
     * identifier used for running threads tied to a project.
     */
    val BACKGROUND_THREAD = "background-thread"

    /**
     * A special key ID used for when we forge a key for a
     * background thread.
     */
    val SUKEY = UUID.fromString("00000000-1234-1234-1234-000000000000")
}

fun ZmlpActor.getAuthorities(): List<GrantedAuthority> {
    return this.permissions.map { SimpleGrantedAuthority(it.name) }
}

fun ZmlpActor.getAuthentication(): Authentication {
    return UsernamePasswordAuthenticationToken(
        this,
        this.keyId,
        this.permissions.map { SimpleGrantedAuthority(it.name) }
    )
}
