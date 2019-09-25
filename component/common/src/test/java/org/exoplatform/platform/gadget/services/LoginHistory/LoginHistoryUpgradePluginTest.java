package org.exoplatform.platform.gadget.services.LoginHistory;

import java.util.Date;
import java.util.List;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.platform.gadget.services.LoginHistory.storage.JCRLoginHistoryStorageImpl;
import org.exoplatform.platform.gadget.services.LoginHistory.storage.LoginHistoryStorage;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.naming.InitialContextInitializer;

@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.platform.test-configuration.xml")
})
public class LoginHistoryUpgradePluginTest extends BaseExoTestCase {

  private JCRLoginHistoryStorageImpl jcrLoginHistoryStorage;

  private LoginHistoryStorage jpaLoginHistoryStorage;

  private RepositoryService repositoryService;

  private EntityManagerService entityManagerService;

  private LoginHistoryService loginHistoryService;

  public void setUp() {
    ExoContainer container = getContainer();
    container.getComponentInstance(InitialContextInitializer.class);

    jcrLoginHistoryStorage = container.getComponentInstanceOfType(JCRLoginHistoryStorageImpl.class);
    jpaLoginHistoryStorage = container.getComponentInstanceOfType(LoginHistoryStorage.class);
    repositoryService = container.getComponentInstanceOfType(RepositoryService.class);
    entityManagerService = container.getComponentInstanceOfType(EntityManagerService.class);
    loginHistoryService = container.getComponentInstanceOfType(LoginHistoryService.class);
  }

  public void testShouldMigrateLoginHistory() throws Exception {
    // Given
    LoginHistoryUpgradePlugin loginHistoryUpgradePlugin = new LoginHistoryUpgradePlugin(new InitParams(),
        jcrLoginHistoryStorage, jpaLoginHistoryStorage, repositoryService, entityManagerService);

    jcrLoginHistoryStorage.addLoginHistoryEntry("upgradeUser1", new Date("Jul 27 2011 13:52:57").getTime());
    jcrLoginHistoryStorage.addLoginHistoryEntry("upgradeUser1", new Date("Aug 10 2011 08:42:39").getTime());
    jcrLoginHistoryStorage.addLoginHistoryEntry("upgradeUser1", new Date("Aug 18 2011 11:23:45").getTime());
    jcrLoginHistoryStorage.addLoginHistoryEntry("upgradeUser1", new Date("Aug 19 2011 07:27:34").getTime());
    jcrLoginHistoryStorage.addLoginHistoryEntry("upgradeUser1", new Date("Aug 20 2011 09:56:12").getTime());
    jcrLoginHistoryStorage.addLoginHistoryEntry("upgradeUser2", new Date("Jul 21 2011 14:07:25").getTime());
    jcrLoginHistoryStorage.addLoginHistoryEntry("upgradeUser2", new Date("Aug 24 2011 17:45:15").getTime());

    // When
    loginHistoryUpgradePlugin.processUpgrade("5.2.0", "5.1.0");

    // Then
    entityManagerService.startRequest(PortalContainer.getInstance());
    try {
      List<LastLoginBean> upgradeUser1LastLogins = loginHistoryService.getLastLogins(10, "upgradeUser1");
      assertNotNull(upgradeUser1LastLogins);
      assertEquals(5, upgradeUser1LastLogins.size());

      long upgradeUser1LastLogin = loginHistoryService.getLastLogin("upgradeUser1");
      assertEquals(new Date("Aug 20 2011 09:56:12").getTime(), upgradeUser1LastLogin);
    } finally {
      entityManagerService.endRequest(PortalContainer.getInstance());
    }
  }
}
