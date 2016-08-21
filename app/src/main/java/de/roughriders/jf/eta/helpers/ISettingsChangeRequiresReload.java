package de.roughriders.jf.eta.helpers;

/**
 * Created by b0wter on 21-Aug-16.
 */
public interface ISettingsChangeRequiresReload {
    void onRequiresReload(String entityName, boolean clearedAll);
}
