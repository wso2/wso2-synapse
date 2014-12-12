package org.apache.synapse.commons.vfs;

public class VFSParamDTO {

    private boolean autoLockRelease;
    private boolean autoLockReleaseSameNode;
    private Long autoLockReleaseInterval;    
    
    public VFSParamDTO(){
        autoLockRelease = false;
        autoLockReleaseSameNode = true;
        autoLockReleaseInterval = null;
    }
    
    /**
     * @return the autoLockRelease
     */
    public boolean isAutoLockRelease() {
        return autoLockRelease;
    }
    /**
     * @param autoLockRelease the autoLockRelease to set
     */
    public void setAutoLockRelease(boolean autoLockRelease) {
        this.autoLockRelease = autoLockRelease;
    }
    /**
     * @return the autoLockReleaseSameNode
     */
    public boolean isAutoLockReleaseSameNode() {
        return autoLockReleaseSameNode;
    }
    /**
     * @param autoLockReleaseSameNode the autoLockReleaseSameNode to set
     */
    public void setAutoLockReleaseSameNode(boolean autoLockReleaseSameNode) {
        this.autoLockReleaseSameNode = autoLockReleaseSameNode;
    }
    /**
     * @return the autoLockReleaseInterval
     */
    public Long getAutoLockReleaseInterval() {
        return autoLockReleaseInterval;
    }
    /**
     * @param autoLockReleaseInterval the autoLockReleaseInterval to set
     */
    public void setAutoLockReleaseInterval(Long autoLockReleaseInterval) {
        this.autoLockReleaseInterval = autoLockReleaseInterval;
    }
    
}
