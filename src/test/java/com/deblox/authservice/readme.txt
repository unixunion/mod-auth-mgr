    vertx run Server.java -cluster
    vertx run RateCounter.java -cluster -cluster-port 8902
    run Client.java -cluster -cluster-port 8005 -instances 8