package com.ilusons.notioff.data

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ProfileTitleValidationTest {

    @Test
    fun trimsWhitespace() {
        assertThat(DataStoreProfileRepository.validateTitle("  Work  ")).isEqualTo("Work")
    }

    @Test
    fun blank_throws() {
        assertThrows(InvalidProfileTitleException::class.java) {
            DataStoreProfileRepository.validateTitle("")
        }
        assertThrows(InvalidProfileTitleException::class.java) {
            DataStoreProfileRepository.validateTitle("   ")
        }
    }

    @Test
    fun tooLong_throws() {
        val long = "x".repeat(DataStoreProfileRepository.MAX_TITLE_LENGTH + 1)
        assertThrows(InvalidProfileTitleException::class.java) {
            DataStoreProfileRepository.validateTitle(long)
        }
    }

    @Test
    fun maxLength_ok() {
        val ok = "x".repeat(DataStoreProfileRepository.MAX_TITLE_LENGTH)
        assertThat(DataStoreProfileRepository.validateTitle(ok)).isEqualTo(ok)
    }
}
