package tools.spirals.cerberus237.adaptationactionsbase.core;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;

public class SampleAdaptationActionTest {

    private SampleAdaptationAction action;

    @Before
    public void setUp() {
        action = new SampleAdaptationAction("action1", "This is a sample action.", true);
    }

    @Test
    public void testPerform_Success() {
        AdaptationActionResult result = action.perform();
        assertEquals(AdaptationActionResult.SUCCESS, result);
    }

    @Test
    public void testPerform_Failure() {
        action = new SampleAdaptationAction("action1", "This is a sample action.", false);
        AdaptationActionResult result = action.perform();
        assertEquals(AdaptationActionResult.FAILURE, result);
    }

    @Test
    public void testGetActionId() {
        assertEquals("action1", action.getActionId());
    }

    @Test
    public void testGetDescription() {
        assertEquals("This is a sample action.", action.getDescription());
    }

    @Test
    public void testCanPerform() {
        assertTrue(action.canPerform());

        action = new SampleAdaptationAction("action1", "This is a sample action.", false);
        assertFalse(action.canPerform());
    }

    public class SampleAdaptationAction implements IAdaptationAction {

        private String actionId;
        private String description;
        private boolean canPerform;

        public SampleAdaptationAction(String actionId, String description, boolean canPerform) {
            this.actionId = actionId;
            this.description = description;
            this.canPerform = canPerform;
        }

        @Override
        public AdaptationActionResult perform() {
            return canPerform ? AdaptationActionResult.SUCCESS : AdaptationActionResult.FAILURE;
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