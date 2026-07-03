@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"donor :: events", "donor :: lookup", "transfer :: events"}
)
package com.blood.notification;
