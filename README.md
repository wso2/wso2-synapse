# WSO2-Synapse
Welcome to the WSO2-Synapse source repository. This repository contains the WSO2 maintained fork of the [Apache Synapse](http://synapse.apache.org/) project. This includes all the new features and bug fixes done by the WSO2 team on top of the stable apache synapse source code. This is the high performing, asynchronous messaging engine used by the WSO2 ESB runtime.

# WSO2-Synapse Features
- High performing, non-blocking HTTP transport (Passthrough) for HTTP based messaging 
- Support for different transport protocols HTTP/S, JMS, File, SMS, TCP, UDP, FIX, POP/IMAP/SMTP, XMPP
- Support for handling different message formats like SOAP, JSON, Binary, POX, XML, Text
- Transforming messages with different methods (XSLT, XQuery, PayloadFactory)
- support for all Enterprise Integration Patterns or EIPs (including scatter/gather, message filters, recipient list, dead-letter channels, guaranteed delivery and message enrichment), database integration, event publishing, logging & auditing, validation
- Securing the services with heterogenous security mechanisms (WS-Sec based)
- Support throttling and caching (QoS) on top of services
- Routing messages based on headers, content and priority 
- Support for running scheduled mediation tasks
- Monitoring and management through JMX
- Extending the capabilities through built in extension points (custom mediators, custom tasks) and scripting languages (Ruby, Javascript, Groovy)

# How to Contribute

* Please report issues at [Github](https://github.com/wso2/product-ei/issues)
* Send your pull requests to [wso2-synapse](https://github.com/wso2/wso2-synapse) repository

# Contact us

WSO2 developers can be contacted via the mailing lists:

* WSO2 Developers List : dev@wso2.org
* WSO2 Architecture List : architecture@wso2.org

## Jenkins Build Status

|  Branch | Build Status |
| :------------ |:-------------
| wso2-synapse master      | [![Build Status](https://wso2.org/jenkins/view/wso2-dependencies/job/forked-dependencies/job/wso2-synapse/badge/icon)](https://wso2.org/jenkins/view/wso2-dependencies/job/forked-dependencies/job/wso2-synapse)
