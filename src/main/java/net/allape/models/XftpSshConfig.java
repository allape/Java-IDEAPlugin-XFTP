package net.allape.models;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ssh.config.unified.SshConfig;
import com.intellij.ssh.config.unified.SshConfigManager;

import java.util.ArrayList;
import java.util.List;

public class XftpSshConfig {

    private final SshConfig config;

    public XftpSshConfig (SshConfig config) {
        this.config = config;
    }

    public SshConfig getConfig() {
        return config;
    }

    public String host () {
        return this.config.getHost();
    }

    public int port () {
        return this.config.getPort();
    }

    public String username () {
        return this.config.getUsername();
    }

    public SshConfig.AuthData auth () {
        return this.config.getAuthDataFromForPasswordSafe();
    }

    @Override
    public String toString() {
        return this.config.getPresentableFullName();
    }

    /**
     * 获取所有配置内容
     * @return 所有配置内容
     */
    public static List<XftpSshConfig> getConfigs () {
        Project project = ProjectManager.getInstance().getDefaultProject();
        List<SshConfig> sshConfigs = SshConfigManager.getInstance(project).getConfigs();
        List<XftpSshConfig> xftpSshConfigs = new ArrayList<>(sshConfigs.size());
        for (SshConfig config : sshConfigs) {
            xftpSshConfigs.add(new XftpSshConfig(config));
        }
        return xftpSshConfigs;
    }

}
