package tools.spirals.cerberus237.adaptationactionsbase.core;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;

public class SampleRollbackableAdaptationActionTest {

    private SampleRollbackableAdaptationAction action;

    @Before
    public void setUp() {
        action = new SampleRollbackableAdaptationAction("action1", "This is a sample rollbackable action.");
    }

    @Test
    public void testPerform_Success() {
        AdaptationActionResult result = action.perform();
        assertEquals(AdaptationActionResult.SUCCESS, result);
    }

    @Test
    public void testPerform_Failure() {
        action = new SampleRollbackableAdaptationAction("action1", "This is a sample rollbackable action.");
        action.canPerform = false; 
        AdaptationActionResult result = action.perform();
        assertEquals(AdaptationActionResult.FAILURE, result);
    }

    @Test
    public void testRollback_Success() {
        action.perform(); 
        AdaptationActionResult result = action.rollback();
        assertEquals(AdaptationActionResult.SUCCESS, result);
    }

    @Test
    public void testRollback_Failure() {
        AdaptationActionResult result = action.rollback(); 
        assertEquals(AdaptationActionResult.FAILURE, result);
    }

    @Test
    public void testGetActionId() {
        assertEquals("action1", action.getActionId());
    }

    @Test
    public void testGetDescription() {
        assertEquals("This is a sample rollbackable action.", action.getDescription());
    }

    @Test
    public void testCanPerform() {
        assertTrue(action.canPerform());

        action = new SampleRollbackableAdaptationAction("action1", "This is a sample rollbackable action.");
        action.canPerform = false; 
        assertFalse(action.canPerform());
    }

    public class SampleRollbackableAdaptationAction implements IRollbackableAdaptationAction {

    private String actionId;
    private String description;
    private boolean canPerform;
    private boolean performed;

    public SampleRollbackableAdaptationAction(String actionId, String description) {
        this.actionId = actionId;
        this.description = description;
        this.canPerform = true;
        this.performed = false;
    }

    @Override
    public AdaptationActionResult perform() {
        if (canPerform) {
            performed = true;
            return AdaptationActionResult.SUCCESS;
        }
        return AdaptationActionResult.FAILURE;
    }

    @Override
    public AdaptationActionResult rollback() {
        if (performed) {
            performed = false;
            return AdaptationActionResult.SUCCESS;
        }
        return AdaptationActionResult.FAILURE;
    }

    @Override
    public String getActionId() {
        return actionId;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean canPerform() {
        return canPerform;
    }
}
}