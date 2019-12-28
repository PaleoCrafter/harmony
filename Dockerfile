FROM nginx:1.17-alpine

COPY proxy.conf /etc/nginx/nginx.conf
