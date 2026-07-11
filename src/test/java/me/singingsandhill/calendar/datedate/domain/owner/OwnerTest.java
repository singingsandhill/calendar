package me.singingsandhill.calendar.datedate.domain.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.singingsandhill.calendar.datedate.application.exception.OwnerAlreadyLinkedException;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;

class OwnerTest {

    @Test
    @DisplayName("Valid owner ID should create Owner successfully")
    void validOwnerId_createsOwner() {
        Owner owner = new Owner("test-user");

        assertThat(owner.getOwnerId()).isEqualTo("test-user");
        assertThat(owner.getCreatedAt()).isNotNull();
        assertThat(owner.getSchedules()).isEmpty();
    }

    @Test
    @DisplayName("Owner ID with numbers and hyphens should be valid")
    void ownerIdWithNumbersAndHyphens_isValid() {
        Owner owner = new Owner("user-123");

        assertThat(owner.getOwnerId()).isEqualTo("user-123");
    }

    @Test
    @DisplayName("Null owner ID should throw exception")
    void nullOwnerId_throwsException() {
        assertThatThrownBy(() -> new Owner(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("Empty owner ID should throw exception")
    void emptyOwnerId_throwsException() {
        assertThatThrownBy(() -> new Owner(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("Owner ID shorter than 2 characters should throw exception")
    void shortOwnerId_throwsException() {
        assertThatThrownBy(() -> new Owner("a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 2 and 20");
    }

    @Test
    @DisplayName("Owner ID longer than 20 characters should throw exception")
    void longOwnerId_throwsException() {
        assertThatThrownBy(() -> new Owner("abcdefghijklmnopqrstuvwxyz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 2 and 20");
    }

    @Test
    @DisplayName("Owner ID with uppercase letters should throw exception")
    void uppercaseOwnerId_throwsException() {
        assertThatThrownBy(() -> new Owner("TestUser"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase");
    }

    @Test
    @DisplayName("Owner ID with special characters should throw exception")
    void specialCharOwnerId_throwsException() {
        assertThatThrownBy(() -> new Owner("test_user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase");
    }

    @Test
    @DisplayName("Owner ID with spaces should throw exception")
    void spacesInOwnerId_throwsException() {
        assertThatThrownBy(() -> new Owner("test user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase");
    }

    @Test
    @DisplayName("미연결 오너는 유저에 연결할 수 있고 멱등이다")
    void linkUserIsIdempotentForSameUser() {
        Owner owner = new Owner("my-crew");

        owner.linkUser(42L);
        owner.linkUser(42L);

        assertThat(owner.getUserId()).isEqualTo(42L);
        assertThat(owner.isLinkedTo(42L)).isTrue();
    }

    @Test
    @DisplayName("이미 다른 유저에 연결된 오너 연결 시도는 409 예외")
    void linkUserRejectsDifferentUser() {
        Owner owner = new Owner("my-crew");
        owner.linkUser(42L);

        assertThatThrownBy(() -> owner.linkUser(43L))
                .isInstanceOf(OwnerAlreadyLinkedException.class);
    }
}
