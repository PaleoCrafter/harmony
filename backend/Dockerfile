FROM node:13-alpine AS modules

ADD ./package.json /app/package.json
ADD ./yarn.lock /app/yarn.lock

WORKDIR /app
RUN yarn install

# Development - watches files
FROM node:13-alpine AS dev

ADD ./ /app
COPY --from=modules /app/node_modules /app/node_modules

WORKDIR /app

CMD yarn install && yarn dev

# Production
FROM node:13-alpine AS prod

COPY --from=dev /app /app
RUN chmod +X /app/bin/www

WORKDIR /app
CMD node /app/bin/www
