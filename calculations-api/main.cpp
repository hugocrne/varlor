#include "oatpp/core/base/Config.hpp"
#include "oatpp/web/server/HttpRouter.hpp"
#include "oatpp/web/server/HttpConnectionHandler.hpp"
#include "oatpp/network/Server.hpp"
#include "oatpp/network/tcp/server/ConnectionProvider.hpp"
#include "oatpp/web/protocol/http/outgoing/ResponseFactory.hpp"

#include <iostream>

class SimpleHandler : public oatpp::web::server::HttpRequestHandler {
public:
    std::shared_ptr<oatpp::web::protocol::http::outgoing::Response> handle(const std::shared_ptr<const oatpp::web::protocol::http::incoming::Request>& request) {
        return oatpp::web::protocol::http::outgoing::ResponseFactory::createResponse(oatpp::web::protocol::http::Status::CODE_200, "Hello from Varlor calculations-api!");
    }
};

int main() {
    try {
        auto router = oatpp::web::server::HttpRouter::createShared();

        auto handler = std::make_shared<SimpleHandler>();
        router->route("GET", "/", handler);

        auto connectionHandler = oatpp::web::server::HttpConnectionHandler::createShared(router);
        auto connectionProvider =
            oatpp::network::tcp::server::ConnectionProvider::createShared(
                {"0.0.0.0", 8000, oatpp::network::Address::IP_4});

        oatpp::network::Server server(connectionProvider, connectionHandler);

        std::cout << "🚀 calculations-api up on http://0.0.0.0:8000/" << std::endl;
        server.run();
    } catch (const std::exception& e) {
        std::cerr << "❌ Exception: " << e.what() << std::endl;
        return 1;
    }

    return 0;
}