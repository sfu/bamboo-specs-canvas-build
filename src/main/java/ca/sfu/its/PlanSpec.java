package ca.sfu.its;

import com.atlassian.bamboo.specs.api.BambooSpec;
import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.builders.plan.PlanIdentifier;
import com.atlassian.bamboo.specs.api.builders.project.Project;
import com.atlassian.bamboo.specs.util.BambooServer;
import com.atlassian.bamboo.specs.util.MapBuilder;
import com.atlassian.bamboo.specs.api.builders.permission.Permissions;
import com.atlassian.bamboo.specs.api.builders.BambooKey;
import com.atlassian.bamboo.specs.api.builders.BambooOid;
import com.atlassian.bamboo.specs.api.builders.docker.DockerConfiguration;
import com.atlassian.bamboo.specs.api.builders.permission.PermissionType;
import com.atlassian.bamboo.specs.api.builders.permission.PlanPermissions;
import com.atlassian.bamboo.specs.api.builders.plan.Job;
import com.atlassian.bamboo.specs.api.builders.plan.Stage;
import com.atlassian.bamboo.specs.api.builders.plan.artifact.Artifact;
import com.atlassian.bamboo.specs.api.builders.plan.artifact.ArtifactSubscription;
import com.atlassian.bamboo.specs.api.builders.plan.branches.BranchCleanup;
import com.atlassian.bamboo.specs.api.builders.plan.branches.PlanBranchManagement;
import com.atlassian.bamboo.specs.api.builders.plan.configuration.AllOtherPluginsConfiguration;
import com.atlassian.bamboo.specs.api.builders.plan.configuration.ConcurrentBuilds;
import com.atlassian.bamboo.specs.api.builders.repository.VcsRepositoryIdentifier;
import com.atlassian.bamboo.specs.builders.task.CheckoutItem;
import com.atlassian.bamboo.specs.builders.task.ScriptTask;
import com.atlassian.bamboo.specs.builders.task.VcsCheckoutTask;

/**
 * Plan configuration for Bamboo.
 *
 * @see <a href="https://confluence.atlassian.com/display/BAMBOO/Bamboo+Specs">Bamboo Specs</a>
 */
@BambooSpec
public class PlanSpec {

  DockerConfiguration dockerConfig =
      new DockerConfiguration().image("simonfraseruniversity/bamboo-build-canvas-lms:1565378885");

  public Plan createPlan() {
    final Plan plan =
        new Plan(
            new Project()
                .oid(new BambooOid("gfkbnmmhs7wj")).key(new BambooKey("CANVAS")).name("Canvas")
                .description("SFU Canvas"),
            "Canvas LMS Core", new BambooKey("CANVASLMS")).pluginConfigurations(
                new ConcurrentBuilds().useSystemWideDefault(false),
                new AllOtherPluginsConfiguration().configuration(new MapBuilder<String, Object>()
                    .put("custom", new MapBuilder<String, Object>().put(
                        "ruby-config-environmentVariables", "")
                        .put("buildExpiryConfig",
                            new MapBuilder<String, Object>().put("duration", "2").put("period",
                                "months").put("expiryTypeNothing", "false").put("labelsToKeep", "")
                                .put("buildsToKeep", "1").put("enabled",
                                    "true")
                                .put("expiryTypeArtifact", "true").build())
                        .put("ruby-config-runtime", "NONE ruby-2.4.4p296").build())
                    .build()))
                .description(
                    "Build plan for core Canvas. This plan is being managed via Bamboo Specs configuration-as-code. Modify https://github.com/sfu/bamboo-specs-canvas-deploy project to update the plan")
                .stages(
                    new Stage("Checkout")
                        .jobs(new Job("Checkout repositories", new BambooKey("JOB1"))
                            .pluginConfigurations(
                                new AllOtherPluginsConfiguration().configuration(
                                    new MapBuilder<String, Object>().put("custom",
                                        new MapBuilder<String, Object>()
                                            .put("auto",
                                                new MapBuilder<String, String>()
                                                    .put("regex", "").put("label", "").build())
                                            .put("buildHangingConfig.enabled", "false")
                                            .put("ncover.path", "")
                                            .put("clover",
                                                new MapBuilder<String, String>().put("path", "")
                                                    .put("license", "").put("useLocalLicenseKey",
                                                        "true")
                                                    .build())
                                            .build())
                                        .build()))
                            .artifacts(new Artifact().name("Code Checkout")
                                .copyPattern("canvas-code.tar").shared(true).required(true))
                            .tasks(
                                new ScriptTask().description("Clean workdir")
                                    .inlineBody("find . -delete"),
                                new VcsCheckoutTask().description("Checkout Default Repository")
                                    .checkoutItems(new CheckoutItem().defaultRepository(),
                                        new CheckoutItem().repository(new VcsRepositoryIdentifier()
                                            .name("instructure/QTIMigrationTool")).path(
                                                "vendor/QTIMigrationTool"),
                                        new CheckoutItem().repository(new VcsRepositoryIdentifier()
                                            .name("instructure/analytics")).path(
                                                "gems/plugins/analytics"),
                                        new CheckoutItem().repository(new VcsRepositoryIdentifier()
                                            .name("sfu/canvas_auth")).path(
                                                "gems/plugins/canvas_auth"),
                                        new CheckoutItem().repository(new VcsRepositoryIdentifier()
                                            .name("sfu/canvas-spaces")).path(
                                                "gems/plugins/canvas_spaces"))
                                    .cleanCheckout(true),
                                new ScriptTask().description("Fix QTIMigrationTool permissions")
                                    .inlineBody("chmod +x vendor/QTIMigrationTool/migrate.py"),
                                new ScriptTask().description("Create tarball")
                                    .inlineBody("tar --exclude=\"*.tar*\" -cf canvas-code.tar ."))),
                    new Stage("Build Canvas").jobs(new Job("Build Canvas", new BambooKey("DEPS"))
                        .pluginConfigurations(new AllOtherPluginsConfiguration().configuration(
                            new MapBuilder<String, Object>().put("custom",
                                new MapBuilder<String, Object>().put("auto",
                                    new MapBuilder<String, String>().put("regex", "")
                                        .put("label", "").build())
                                    .put("buildHangingConfig.enabled", "false")
                                    .put("ncover.path", "")
                                    .put("clover", new MapBuilder<String, String>().put("path", "")
                                        .put("license", "").put("useLocalLicenseKey", "true")
                                        .build())
                                    .build())
                                .build()))
                        .artifacts(
                            new Artifact()
                                .name("Canvas Release").copyPattern("canvas-release.tar")
                                .shared(true).required(true))
                        .tasks(new ScriptTask().description(
                            "Clean and untar artifact").inlineBody(
                                "find . -not -name '*.tar*' -delete\ntar xf canvas-code.tar\nrm -f canvas-code.tar"),
                            new ScriptTask().description("Bundle Install").inlineBody(
                                "bundle config build.nokogiri --use-system-libraries\nbundle config build.pg --with-pg-config=/usr/pgsql-9.6/bin/pg_config\nbundle install --binstubs --path=vendor/bundle --without=mysql,sqlite"),
                            new ScriptTask().description("Compile assets")
                                .inlineBody("bundle exec rake canvas:compile_assets --trace")
                                .environmentVariables("RAILS_ENV=production"),
                            new ScriptTask().description("Add VERSION file to tarball")
                                .inlineBody("git rev-parse HEAD > VERSION"),
                            new ScriptTask().description("Remove unnecessary files for release")
                                .inlineBody(
                                    "rm -rf .git spec tmp log public/dist/webpack-dev client_apps/*/node_modules gems/plugins/*/spec_canvas && cd node_modules && ls -A | grep -v canvas_offline_course_viewer | xargs rm -rf"),
                            new ScriptTask().description("Create tarball")
                                .inlineBody("tar --exclude=\"*.tar*\" -cf canvas-release.tar ."))
                        .artifactSubscriptions(new ArtifactSubscription().artifact("Code Checkout"))
                        .dockerConfiguration(dockerConfig)))
                .linkedRepositories("sfu/canvas-lms-internal", "instructure/QTIMigrationTool",
                    "instructure/analytics", "sfu/canvas-spaces", "sfu/canvas_auth")

                .planBranchManagement(new PlanBranchManagement().delete(new BranchCleanup())
                    .notificationForCommitters())
                .forceStopHungBuilds();

    return plan;
  }

  public static void main(final String[] args) throws Exception {
    // By default credentials are read from the '.credentials' file.
    final BambooServer bambooServer = new BambooServer("https://bamboo-nsx.its.sfu.ca");
    final PlanSpec planSpec = new PlanSpec();

    final Plan plan = planSpec.createPlan();
    bambooServer.publish(plan);

    final PlanPermissions planPermission = planSpec.planPermission();
    bambooServer.publish(planPermission);
  }

  public PlanPermissions planPermission() {
    final PlanPermissions planPermission =
        new PlanPermissions(new PlanIdentifier("CASMFA", "MFAMGR")).permissions(new Permissions()
            .userPermissions("grahamb", PermissionType.EDIT, PermissionType.VIEW,
                PermissionType.ADMIN, PermissionType.CLONE, PermissionType.BUILD)
            .loggedInUserPermissions(PermissionType.VIEW).anonymousUserPermissionView());
    return planPermission;
  }

}
