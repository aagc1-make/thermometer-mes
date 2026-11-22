package com.ktg.system.domain;

import java.util.Date;

/** 用户管理可见的人脸录入状态与登录策略。 */
public class SysUserFaceStatus
{
    private boolean enrolled;

    private boolean enabled;

    private Date updatedAt;

    public boolean isEnrolled()
    {
        return enrolled;
    }

    public void setEnrolled(boolean enrolled)
    {
        this.enrolled = enrolled;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public Date getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt)
    {
        this.updatedAt = updatedAt;
    }
}
