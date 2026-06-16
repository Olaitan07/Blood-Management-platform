@ApplicationModule(
        allowedDependencies = {
                "hospital :: service",
                "transfer :: reporting",
                "inventory :: reporting",
                "donor :: reporting"
        }
)
package com.blood.report;

import org.springframework.modulith.ApplicationModule;
