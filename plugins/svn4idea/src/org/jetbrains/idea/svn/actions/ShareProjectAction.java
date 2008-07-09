package org.jetbrains.idea.svn.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.idea.svn.SvnBundle;
import org.jetbrains.idea.svn.SvnVcs;
import org.jetbrains.idea.svn.SvnWorkingCopyFormatHolder;
import org.jetbrains.idea.svn.checkout.SvnCheckoutProvider;
import org.jetbrains.idea.svn.dialogs.ShareDialog;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;

public class ShareProjectAction extends BasicAction {

  protected String getActionName(AbstractVcs vcs) {
    return SvnBundle.message("share.directory.action");
  }

  public void update(AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    final DataContext dataContext = e.getDataContext();

    Project project = PlatformDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      presentation.setEnabled(false);
      presentation.setVisible(false);
      return;
    }

    VirtualFile[] files = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
    if (files == null || files.length == 0) {
      presentation.setEnabled(false);
      presentation.setVisible(false);
      return;
    }
    boolean enabled = false;
    boolean visible = false;
    if (files.length == 1 && files [0].isDirectory()) {
      visible = true;
      if (!SVNWCUtil.isVersionedDirectory(new File(files [0].getPath()))) {
        enabled = true;
      }
    }
    presentation.setEnabled(enabled);
    presentation.setVisible(visible);
  }

  protected boolean isEnabled(Project project, SvnVcs vcs, VirtualFile file) {
    return false;
  }

  protected boolean needsFiles() {
    return true;
  }

  protected void perform(final Project project, final SvnVcs activeVcs, final VirtualFile file, DataContext context) throws VcsException {
    ShareDialog shareDialog = new ShareDialog(project);
    shareDialog.show();

    final String parent = shareDialog.getSelectedURL();
    if (shareDialog.isOK() && parent != null) {
      final SVNException[] error = new SVNException[1];
      ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {

        public void run() {
          try {
            SVNURL url = SVNURL.parseURIEncoded(parent).appendPath(file.getName(), false);
            SVNCommitInfo info = activeVcs.createCommitClient().doMkDir(new SVNURL[] {url},
                                                                        SvnBundle.message("share.directory.commit.message", file.getName()));
            SVNRevision revision = SVNRevision.create(info.getNewRevision());
            final File path = new File(file.getPath());
            SvnCheckoutProvider.promptForWCopyFormat(path, project);
            activeVcs.createUpdateClient().doCheckout(url, path, SVNRevision.UNDEFINED, revision, true);
            SvnWorkingCopyFormatHolder.setPresetFormat(null);
            activeVcs.createWCClient().doAdd(new File(file.getPath()), true, false, false, true);
          } catch (SVNException e) {
            error[0] = e;
          } finally {
            SvnWorkingCopyFormatHolder.setPresetFormat(null);
          }
        }
      }, SvnBundle.message("share.directory.title"), false, project);
      if (error[0] != null) {
        throw new VcsException(error[0].getMessage());
      }
      Messages.showInfoMessage(project, SvnBundle.message("share.directory.info.message", file.getName()),
                               SvnBundle.message("share.directory.title"));
    }

  }

  protected void batchPerform(Project project, final SvnVcs activeVcs, VirtualFile[] file, DataContext context) throws VcsException {
  }

  protected boolean isBatchAction() {
    return false;
  }
}
