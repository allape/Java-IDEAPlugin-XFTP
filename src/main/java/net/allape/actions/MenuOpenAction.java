package net.allape.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ssh.config.unified.SshConfig;
import com.intellij.ssh.config.unified.SshConfigManager;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;

public class MenuOpenAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            // 工具包
//            RemoteSdkUtil.getSshConfigList();
            Class util = Class.forName("com.jetbrains.plugins.remotesdk.RemoteSdkUtil");
            Method getSshConfigList = util.getDeclaredMethod("getSshConfigList", Project.class);
            getSshConfigList.setAccessible(true);

            Project project = ProjectManager.getInstance().getDefaultProject();
//            List<SshConfig> sshConfigs = SshConfigManager.getInstance(project).getConfigs();
            List<SshConfig> sshConfigs = (List<SshConfig>) getSshConfigList.invoke(null, project);
            for (SshConfig config : sshConfigs) {
                System.out.println(config.getUsername() + "@" + config.getHost());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
