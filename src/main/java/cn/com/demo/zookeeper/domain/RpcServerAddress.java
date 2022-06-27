package cn.com.demo.zookeeper.domain;

public class RpcServerAddress {
        private String ip;
        private int port;
        private String path;

        /**
         * @return
         */
        public String getIp() {
            return ip;
        }

        /**
         * @param ip
         */
        public void setIp(String ip) {
            this.ip = ip;
        }

        /**
         * @return
         */
        public int getPort() {
            return port;
        }

        /**
         * @param port
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * @return
         */
        public String getPath() {
            return path;
        }

        /**
         * @param path
         */
        public void setPath(String path) {
            this.path = path;
        }

        /**
         *默认构造函数
         */
        public RpcServerAddress() {
        }

        /**
         * @param ip
         * @param port
         * @param path
         */
        public RpcServerAddress(String ip, int port, String path) {
            this.ip = ip;
            this.port = port;
            this.path = path;
        }

        /**
         * 比较两个对象是否相等
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RpcServerAddress) {
                RpcServerAddress rpcServerAddress = (RpcServerAddress) obj;
                if (this.ip == rpcServerAddress.ip && this.port == rpcServerAddress.port && this.path == rpcServerAddress.path) {
                    return true;
                }
                return false;
            }
            return false;
        }
}
