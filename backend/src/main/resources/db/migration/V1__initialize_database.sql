CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE system_information (
                                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    application_name VARCHAR(150) NOT NULL,
                                    application_version VARCHAR(50) NOT NULL,
                                    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_information (
    application_name,
    application_version
)
VALUES (
           'Vednex AI Business Suite',
           '0.1.0'
       );