FROM nginx:stable

COPY ./100MB.zip /var/www/100MB.zip
COPY ./nginx.conf /etc/nginx/conf.d/default.conf

CMD ["nginx", "-g", "daemon off;"]
