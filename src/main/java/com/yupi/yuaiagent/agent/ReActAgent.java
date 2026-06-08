package com.yupi.yuaiagent.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct agent base class.
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public abstract class ReActAgent extends BaseAgent {

    /**
     * Think and decide whether an action is required.
     *
     * @return true when act should be executed
     */
    public abstract boolean think();

    /**
     * Execute the selected action.
     *
     * @return action result
     */
    public abstract String act();

    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                return "Thinking finished, no action needed";
            }
            return act();
        } catch (Exception e) {
            log.error("Step execution failed", e);
            return "Step execution failed: " + e.getMessage();
        }
    }
}
