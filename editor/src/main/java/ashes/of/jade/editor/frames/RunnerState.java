package ashes.of.jade.editor.frames;

public class RunnerState {

    /**
     * Timeout for background eval, in ms
     */
    private static final long BACKGROUND_EVAL_TIMEOUT = 2000;

    private boolean runNow;
    private long lastUpdateTime = System.currentTimeMillis();
    private long lastEvaluatedTime;

    public boolean isRunNow() {
        return runNow;
    }

    public void setRunNow(boolean runNow) {
        this.runNow = runNow;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public long getLastEvaluatedTime() {
        return lastEvaluatedTime;
    }

    public void updateLastEvaluatedTime() {
        this.lastEvaluatedTime = lastUpdateTime;
    }

    public void updateTime() {
        lastUpdateTime = System.currentTimeMillis();
    }

    public boolean canRunInBackground() {
        return !runNow && System.currentTimeMillis() - lastUpdateTime > BACKGROUND_EVAL_TIMEOUT &&
                lastUpdateTime != lastEvaluatedTime;
    }
}
