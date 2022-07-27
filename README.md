# How to use swagger endpoints in openshift environment

![img.png](/infra/docs/openshift.png)
1. Open openshift website, then find in right corner and press the "Copy login command" below your profile name 
2. Run the command in command line `oc login --token=XXX --server=XXX`
3. Select correct project `oc project XXX`
4. Find pod name, CHANGE_ME to events returns first events-api pod name `oc get pods | grep CHANGE_ME | awk '{print $1}'`
5. Open tunnel to pod in port 8080, `oc port-forward POD_NAME 8080:8080`
6. Then you can open swagger on your local computer in [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) 

## How to setup core application
[Read this!](/infra/docs/Readme.MD)

## How to use swagger endpoint using infra/runConfigurations on intellij idea ultimate.

* [Events-API swagger](http://localhost:8080/swagger-ui/index.html)
* [Cart-API swagger](http://localhost:8180/swagger-ui/index.html)
* [Message-API swagger](http://localhost:8181/swagger-ui/index.html)
* [Mock-API swagger](http://localhost:8182/swagger-ui/index.html)
* [Order-API swagger](http://localhost:8183/swagger-ui/index.html)
* [Payment-API swagger](http://localhost:8184/swagger-ui/index.html)
* [Price-API swagger](http://localhost:8185/swagger-ui/index.html)
* [Product-API swagger](http://localhost:8186/swagger-ui/index.html)
* [Mapping/ServiceConfiguration-API swagger](http://localhost:8187/swagger-ui/index.html)
* [Merchant-API swagger](http://localhost:8188/swagger-ui/index.html)
* [History-API swagger](http://localhost:8189/swagger-ui/index.html)


## How to use swagger endpoint using `docker compose up` command

* [cartapi docker swagger](http://localhost:8180/swagger-ui/index.html)
* [eventsapi docker swagger](http://localhost:8181/swagger-ui/index.html)
* [historyapi docker swagger](http://localhost:8182/swagger-ui/index.html)
* [merchantapi docker swagger](http://localhost:8183/swagger-ui/index.html)
* [messageapi docker swagger](http://localhost:8184/swagger-ui/index.html)
* [mockproductmanagement docker swagger](http://localhost:8185/swagger-ui/index.html)
* [orderapi docker swagger](http://localhost:8186/swagger-ui/index.html)
* [paymentapi docker swagger](http://localhost:8187/swagger-ui/index.html)
* [priceapi docker swagger](http://localhost:8188/swagger-ui/index.html)
* [productapi docker swagger](http://localhost:8189/swagger-ui/index.html)
* [productmapping docker swagger](http://localhost:8190/swagger-ui/index.html)