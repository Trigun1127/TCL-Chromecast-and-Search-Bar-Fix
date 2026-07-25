package android.content.pm;

import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/**
 * Compile-only signature stub. Do not package this class into the runtime DEX.
 */
public interface IPackageManager extends IInterface {
    void deletePackageVersioned(
            VersionedPackage versionedPackage,
            IPackageDeleteObserver2 observer,
            int userId,
            int flags) throws RemoteException;

    abstract class Stub {
        public static IPackageManager asInterface(IBinder binder) {
            return null;
        }
    }
}
