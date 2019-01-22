package org.exoplatform.platform.gadget.services.LoginHistory;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.platform.gadget.services.LoginHistory.storage.JCRLoginHistoryStorageImpl;
import org.exoplatform.platform.gadget.services.LoginHistory.storage.LoginHistoryStorage;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

public class LoginHistoryUpgradePlugin extends UpgradeProductPlugin {
  private static final Log           LOG                          = ExoLogger.getLogger(LoginHistoryUpgradePlugin.class);

  private static final int           LOGIN_HISTORY_NODE_PAGE_SIZE = 50;

  private JCRLoginHistoryStorageImpl jcrLoginHistoryStorage;

  private LoginHistoryStorage        jpaLoginHistoryStorage;

  private RepositoryService          repositoryService;

  private EntityManagerService       entityManagerService;

  public LoginHistoryUpgradePlugin(InitParams initParams,
                                   JCRLoginHistoryStorageImpl jcrLoginHistoryStorage,
                                   LoginHistoryStorage jpaLoginHistoryStorage,
                                   RepositoryService repositoryService,
                                   EntityManagerService entityManagerService) {
    super(initParams);
    this.jcrLoginHistoryStorage = jcrLoginHistoryStorage;
    this.jpaLoginHistoryStorage = jpaLoginHistoryStorage;
    this.repositoryService = repositoryService;
    this.entityManagerService = entityManagerService;
  }

  @Override
  public void processUpgrade(String newVersion, String previousVersion) {
    // First check to see if the JCR still contains Login History data. If not,
    // migration is skipped

    int errors = 0;

    if (!hasDataToMigrate()) {
      LOG.info("No Login History data to migrate from JCR to RDBMS");
    } else {
      LOG.info("== Start migration of Login History data from JCR to RDBMS");

      errors = migrateAndDeleteLoginHistory();

      if (errors == 0) {
        deleteLoginHistoryCounters();
        LOG.info("==    Login History migration - Entries and Counters JCR Data deleted successfully");

        try {
          jcrLoginHistoryStorage.removeLoginHistoryHomeNode();
        } catch (Exception e) {
          throw new RuntimeException("Error when deleting Login History home node");
        }
        LOG.info("==    Login History migration - Home Node deleted successfully !");
        LOG.info("==    Login History migration done");
      } else {
        LOG.error("==    Login History migration aborted, {} errors encountered", errors);
        throw new RuntimeException("Login History migration aborted because of migration failures");
      }
    }
  }

  private Session getSession(SessionProvider sessionProvider) throws Exception {
    ManageableRepository currentRepo = this.repositoryService.getCurrentRepository();
    return sessionProvider.getSession(currentRepo.getConfiguration().getDefaultWorkspaceName(), currentRepo);
  }

  private Boolean hasDataToMigrate() {
    boolean hasDataToMigrate;
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      Session session = getSession(sProvider);
      hasDataToMigrate = session.getRootNode().hasNode("exo:LoginHistoryHome");
    } catch (Exception e) {
      LOG.error("Error while checking the existence of login history home node", e);
      hasDataToMigrate = false;
    } finally {
      sProvider.close();
    }
    return hasDataToMigrate;
  }

  /**
   * iterates on all present Login History nodes by a given page size, for each
   * returned page of Login History nodes it iterates on each node in order to add
   * it to the JPAStorage and then removes it
   */
  private int migrateAndDeleteLoginHistory() {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    entityManagerService.startRequest(ExoContainerContext.getCurrentContainer());

    long offset = 0, migrated = 0;
    int errors = 0;

    long count;
    try {
      do {
        entityManagerService.endRequest(ExoContainerContext.getCurrentContainer());
        entityManagerService.startRequest(ExoContainerContext.getCurrentContainer());
        NodeIterator loginHistoryNodes = jcrLoginHistoryStorage.getLoginHistoryNodes(sProvider,
                                                                                     offset,
                                                                                     LOGIN_HISTORY_NODE_PAGE_SIZE);
        count = loginHistoryNodes.getSize();
        Node loginHistoryNode;
        String userId = null;
        long loginTime = 0;
        while (loginHistoryNodes.hasNext()) {
          try {
            loginHistoryNode = loginHistoryNodes.nextNode();
            userId = loginHistoryNode.getProperty("exo:LoginHisSvc_loginHistoryItem_userId").getString();
            loginTime = loginHistoryNode.getProperty("exo:LoginHisSvc_loginHistoryItem_loginTime").getLong();
            LOG.debug("==   Login History migration - Migrate entry of user {} at {}", userId, loginTime);
            jpaLoginHistoryStorage.addLoginHistoryEntry(userId, loginTime);
            jcrLoginHistoryStorage.removeLoginHistoryNode(sProvider, loginHistoryNode);
            migrated++;
          } catch (Exception e) {
            LOG.error("==   Login History migration - Error while migrating login of " + userId + " at " + loginTime, e);
            errors++;
          }
        }
        offset += count;
        LOG.info("==   Login History migration - Progress : {} logins migrated ({} errors)", migrated, errors);
      } while (count == LOGIN_HISTORY_NODE_PAGE_SIZE);
    } finally {
      entityManagerService.endRequest(ExoContainerContext.getCurrentContainer());
      sProvider.close();
    }
    return errors;
  }

  /**
   * iterates on Login History Counters by a given page size each time and removes
   * them one by one
   */
  private void deleteLoginHistoryCounters() {
    long removed = 0, offset = 0;
    do {
      try {
        removed = jcrLoginHistoryStorage.removeLoginCounter(offset, LOGIN_HISTORY_NODE_PAGE_SIZE);
        offset += removed;
      } catch (Exception e) {
        LOG.error("==   Login History migration - Error while deleting Login Counter", e);
      }
    } while (removed == LOGIN_HISTORY_NODE_PAGE_SIZE);
  }

}
