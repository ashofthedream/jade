package ashes.of.jade.editor.frames;

public class RunnerState {

    /**
     * Timeout for background eval, in ms
     */
    private static final long BACKGROUND_EVAL_TIMEOUT = 2000;

    private boolean runNow;
    private long lastUpdateTime;
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

    public void setLastEvaluatedTime(long lastEvaluatedTime) {
        this.lastEvaluatedTime = lastEvaluatedTime;
    }

    public void updateTime() {
        lastUpdateTime = System.currentTimeMillis();
    }

    public boolean canRunInBackground(long currentTime) {
        return !runNow &&  currentTime - lastUpdateTime > BACKGROUND_EVAL_TIMEOUT &&
                lastUpdateTime != lastEvaluatedTime;
    }
}
