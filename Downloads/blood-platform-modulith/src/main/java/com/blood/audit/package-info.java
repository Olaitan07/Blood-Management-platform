@ApplicationModule(
        allowedDependencies = {
                "donor :: events",
                "transfer :: events",
                "hospital :: events",
                "auth :: events"
        }
)
package com.blood.audit;

import org.springframework.modulith.ApplicationModule;
