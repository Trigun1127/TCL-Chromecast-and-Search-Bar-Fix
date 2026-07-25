import android.content.Intent;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageManager;
import android.content.pm.VersionedPackage;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Narrow repair stage for the TCL Cast receiver's corrupt per-user package state.
 *
 * This program performs one operation only: it asks Android Package Manager to
 * mark the exact factory Cast receiver version uninstalled for user 0 while
 * retaining its app data. The read-only /product APK cannot be removed by this
 * per-user operation. A separate, standard `cmd package install-existing`
 * command is intentionally required to restore it with hidden=false.
 */
public final class CastPackageRepairStage1 {
    private static final String BUILD_ID = "tcl-cast-state-repair-stage1-v1";
    private static final String TARGET_PACKAGE = "com.google.android.apps.mediashell";
    private static final long TARGET_VERSION = 446070212L;
    private static final int TARGET_USER = 0;

    private static final int DELETE_KEEP_DATA = 0x00000001;
    private static final int DELETE_SYSTEM_APP = 0x00000004;
    private static final int DELETE_FLAGS = DELETE_KEEP_DATA | DELETE_SYSTEM_APP;
    private static final int DELETE_SUCCEEDED = 1;

    private static final String CONFIRMATION =
            "EXECUTE_ONLY_com.google.android.apps.mediashell"
                    + "_USER_0_VERSION_446070212_FLAGS_5";

    private CastPackageRepairStage1() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !CONFIRMATION.equals(args[0])) {
            System.err.println("Refusing: exact repair confirmation argument was not supplied.");
            System.exit(64);
        }

        /*
         * Parse the expected values at runtime so javac/D8 cannot fold this
         * safety check away as a comparison between compile-time constants.
         */
        if (TARGET_USER != Integer.parseInt("0")
                || TARGET_VERSION != Long.parseLong("446070212")
                || DELETE_FLAGS != Integer.parseInt("5")
                || !"com.google.android.apps.mediashell".equals(TARGET_PACKAGE)) {
            System.err.println("Refusing: compiled safety constants do not match.");
            System.exit(78);
        }

        IBinder packageBinder = ServiceManager.getService("package");
        if (packageBinder == null) {
            System.err.println("Package Manager binder is unavailable.");
            System.exit(69);
        }

        IPackageManager packageManager = IPackageManager.Stub.asInterface(packageBinder);
        if (packageManager == null) {
            System.err.println("Could not obtain IPackageManager.");
            System.exit(69);
        }

        CountDownLatch completion = new CountDownLatch(1);
        int[] resultCode = new int[] {Integer.MIN_VALUE};
        String[] resultPackage = new String[] {null};
        String[] resultMessage = new String[] {null};
        boolean[] userActionRequested = new boolean[] {false};

        IPackageDeleteObserver2 observer = new IPackageDeleteObserver2.Stub() {
            @Override
            public void onUserActionRequired(Intent intent) {
                userActionRequested[0] = true;
                resultMessage[0] = "Unexpected user action request";
                completion.countDown();
            }

            @Override
            public void onPackageDeleted(String packageName, int code, String message)
                    throws RemoteException {
                resultPackage[0] = packageName;
                resultCode[0] = code;
                resultMessage[0] = message;
                completion.countDown();
            }
        };

        System.out.println("repair.build=" + BUILD_ID);
        System.out.println("repair.package=" + TARGET_PACKAGE);
        System.out.println("repair.user=" + TARGET_USER);
        System.out.println("repair.versionGuard=" + TARGET_VERSION);
        System.out.println("repair.flags=" + DELETE_FLAGS);
        System.out.println("repair.begin=true");

        packageManager.deletePackageVersioned(
                new VersionedPackage(TARGET_PACKAGE, TARGET_VERSION),
                observer,
                TARGET_USER,
                DELETE_FLAGS);

        if (!completion.await(30, TimeUnit.SECONDS)) {
            System.err.println("repair.timeout=true");
            System.err.println("State is unknown; inspect it before any further action.");
            System.exit(70);
        }

        System.out.println("observer.package=" + resultPackage[0]);
        System.out.println("observer.code=" + resultCode[0]);
        System.out.println("observer.message=" + resultMessage[0]);

        if (userActionRequested[0]) {
            System.err.println("repair.success=false");
            System.err.println("Refusing to treat a user-action request as success.");
            System.exit(77);
        }
        if (!TARGET_PACKAGE.equals(resultPackage[0])) {
            System.err.println("repair.success=false");
            System.err.println("Observer returned an unexpected package.");
            System.exit(74);
        }
        if (resultCode[0] != DELETE_SUCCEEDED) {
            System.err.println("repair.success=false");
            System.exit(1);
        }

        System.out.println("repair.success=true");
        System.out.println("repair.next=verify_then_install_existing");
    }
}
