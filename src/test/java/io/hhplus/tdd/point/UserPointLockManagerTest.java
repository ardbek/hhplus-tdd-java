package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UserPointLockManagerTest {

    private final UserPointLockManager lockManager = new UserPointLockManager();

    @Test
    @DisplayName("같은 userId로 요청이 발생하면 같은 lock 인스턴스를 반환한다.")
    void sameUserId_returnsSameLockInstance() {
        // given
        long userId = 1L;

        // when
        ReentrantLock lock1 = lockManager.getLock(userId);
        ReentrantLock lock2 = lockManager.getLock(userId);

        // then
        assertThat(lock1).isSameAs(lock2);
    }

    @Test
    @DisplayName("다른 userId로 요청이 발생하면 다른 lock 인스턴스를 반환한다.")
    void differentUserId_returnsDifferentLockInstance() {
        // given
        long userId1 = 1L;
        long userId2 = 2L;

        // when
        ReentrantLock lock1 = lockManager.getLock(userId1);
        ReentrantLock lock2 = lockManager.getLock(userId2);

        // then
        assertThat(lock1).isNotSameAs(lock2);
    }
}
