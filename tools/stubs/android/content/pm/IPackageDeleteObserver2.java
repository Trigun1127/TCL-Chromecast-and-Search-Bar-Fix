package android.content.pm;

import android.content.Intent;
import android.os.IInterface;
import android.os.RemoteException;

/**
 * Compile-only signature stub. Do not package this class into the runtime DEX.
 */
public interface IPackageDeleteObserver2 extends IInterface {
    void onUserActionRequired(Intent intent) throws RemoteException;

    void onPackageDeleted(String packageName, int returnCode, String message)
            throws RemoteException;

    abstract class Stub implements IPackageDeleteObserver2 {
        public Stub() {
        }
    }
}
