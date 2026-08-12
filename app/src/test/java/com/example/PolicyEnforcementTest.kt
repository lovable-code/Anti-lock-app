package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.AuthenticatedRemoteCommand
import com.example.util.CommandLifecycleState
import com.example.util.PolicyEnforcementManager
import com.example.util.SecurityPolicyState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PolicyEnforcementTest {

    private lateinit var context: Context
    private val testScope = TestScope()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("sentinel_policy_state", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun `default policy state is NORMAL`() {
        val state = PolicyEnforcementManager.getCurrentPolicyState(context)
        assertEquals(SecurityPolicyState.NORMAL, state)
        assertFalse(PolicyEnforcementManager.isPolicyLocked(context))
    }

    @Test
    fun `setPolicyState updates state and remote policy locked preference`() {
        PolicyEnforcementManager.setPolicyState(context, SecurityPolicyState.LOCKED, "Unit test lock")
        assertEquals(SecurityPolicyState.LOCKED, PolicyEnforcementManager.getCurrentPolicyState(context))
        assertTrue(PolicyEnforcementManager.isPolicyLocked(context))

        PolicyEnforcementManager.setPolicyState(context, SecurityPolicyState.NORMAL, "Unit test unlock")
        assertEquals(SecurityPolicyState.NORMAL, PolicyEnforcementManager.getCurrentPolicyState(context))
        assertFalse(PolicyEnforcementManager.isPolicyLocked(context))
    }

    @Test
    fun `isLockedState correctly categorizes all policy states`() {
        assertTrue(PolicyEnforcementManager.isLockedState(SecurityPolicyState.LOCKED))
        assertTrue(PolicyEnforcementManager.isLockedState(SecurityPolicyState.OFFLINE_LOCKED))
        assertTrue(PolicyEnforcementManager.isLockedState(SecurityPolicyState.LOST))
        assertTrue(PolicyEnforcementManager.isLockedState(SecurityPolicyState.LOCK_PENDING))

        assertFalse(PolicyEnforcementManager.isLockedState(SecurityPolicyState.NORMAL))
        assertFalse(PolicyEnforcementManager.isLockedState(SecurityPolicyState.UNENROLLED))
        assertFalse(PolicyEnforcementManager.isLockedState(SecurityPolicyState.ENROLLED))
        assertFalse(PolicyEnforcementManager.isLockedState(SecurityPolicyState.UNLOCK_PENDING))
        assertFalse(PolicyEnforcementManager.isLockedState(SecurityPolicyState.POLICY_ERROR))
    }

    @Test
    fun `executeAuthenticatedCommand LOCK command transitions state to LOCKED and returns ACKNOWLEDGED`() = runTest {
        val command = AuthenticatedRemoteCommand(
            commandId = "cmd-lock-101",
            deviceId = "sentinel-agent-local",
            commandType = "LOCK_DEVICE"
        )

        val status = PolicyEnforcementManager.executeAuthenticatedCommand(context, command, testScope)
        assertEquals(CommandLifecycleState.ACKNOWLEDGED, status)
        assertEquals(SecurityPolicyState.LOCKED, PolicyEnforcementManager.getCurrentPolicyState(context))
        assertTrue(PolicyEnforcementManager.isPolicyLocked(context))
    }

    @Test
    fun `executeAuthenticatedCommand prevents duplicate command replay`() = runTest {
        val command = AuthenticatedRemoteCommand(
            commandId = "cmd-lock-102",
            deviceId = "sentinel-agent-local",
            commandType = "LOCK_DEVICE"
        )

        val firstStatus = PolicyEnforcementManager.executeAuthenticatedCommand(context, command, testScope)
        assertEquals(CommandLifecycleState.ACKNOWLEDGED, firstStatus)

        // Second execution of the identical commandId should be rejected
        val duplicateStatus = PolicyEnforcementManager.executeAuthenticatedCommand(context, command, testScope)
        assertEquals(CommandLifecycleState.FAILED, duplicateStatus)
    }

    @Test
    fun `executeAuthenticatedCommand rejects expired command`() = runTest {
        val expiredCommand = AuthenticatedRemoteCommand(
            commandId = "cmd-lock-103",
            deviceId = "sentinel-agent-local",
            commandType = "LOCK_DEVICE",
            expiresAt = System.currentTimeMillis() - 10000L
        )

        val status = PolicyEnforcementManager.executeAuthenticatedCommand(context, expiredCommand, testScope)
        assertEquals(CommandLifecycleState.FAILED, status)
        assertEquals(SecurityPolicyState.NORMAL, PolicyEnforcementManager.getCurrentPolicyState(context))
    }

    @Test
    fun `executeAuthenticatedCommand rejects unknown command type`() = runTest {
        val invalidCommand = AuthenticatedRemoteCommand(
            commandId = "cmd-invalid-104",
            deviceId = "sentinel-agent-local",
            commandType = "EXPLODE_DEVICE"
        )

        val status = PolicyEnforcementManager.executeAuthenticatedCommand(context, invalidCommand, testScope)
        assertEquals(CommandLifecycleState.FAILED, status)
    }

    @Test
    fun `authorizeLocalUnlock unlocks with valid device-specific recovery token`() {
        PolicyEnforcementManager.setPolicyState(context, SecurityPolicyState.LOCKED, "Lock before unlock test")
        assertTrue(PolicyEnforcementManager.isPolicyLocked(context))

        val recoveryToken = PolicyEnforcementManager.getDeviceSpecificRecoveryToken(context)
        val result = PolicyEnforcementManager.authorizeLocalUnlock(context, recoveryToken)
        assertTrue(result)
        assertEquals(SecurityPolicyState.NORMAL, PolicyEnforcementManager.getCurrentPolicyState(context))
        assertFalse(PolicyEnforcementManager.isPolicyLocked(context))
    }

    @Test
    fun `authorizeLocalUnlock fails with invalid PIN`() {
        PolicyEnforcementManager.setPolicyState(context, SecurityPolicyState.LOCKED, "Lock before invalid unlock test")
        assertTrue(PolicyEnforcementManager.isPolicyLocked(context))

        val result = PolicyEnforcementManager.authorizeLocalUnlock(context, "0000")
        assertFalse(result)
        assertEquals(SecurityPolicyState.LOCKED, PolicyEnforcementManager.getCurrentPolicyState(context))
        assertTrue(PolicyEnforcementManager.isPolicyLocked(context))
    }

    @Test
    fun `getDpcStatusMap returns complete diagnostic mapping`() {
        val map = PolicyEnforcementManager.getDpcStatusMap(context)
        assertTrue(map.containsKey("DEVICE OWNER"))
        assertTrue(map.containsKey("DPC ACTIVE"))
        assertTrue(map.containsKey("LOCK TASK PERMITTED"))
        assertTrue(map.containsKey("LOCK TASK ACTIVE"))
        assertTrue(map.containsKey("ENROLLMENT"))
        assertTrue(map.containsKey("CLOUD CONNECTION"))
        assertTrue(map.containsKey("LOCAL POLICY"))
        assertTrue(map.containsKey("REMOTE POLICY"))
        assertTrue(map.containsKey("OPERATING MODE"))
        assertEquals("CONSUMER MODE", map["OPERATING MODE"])
    }
}
