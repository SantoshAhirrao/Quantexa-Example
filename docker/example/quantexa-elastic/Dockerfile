FROM elasticsearch:5.2.2

RUN bin/elasticsearch-plugin install repository-s3

ADD synonyms /usr/share/elasticsearch/config/analysis
ADD limits.conf /etc/security/limits.conf

VOLUME /usr/share/elasticsearch/data/quantexa

RUN echo "cluster.name: quantexa" >> /usr/share/elasticsearch/config/elasticsearch.yml
RUN echo "network.host: 0.0.0.0" >> /usr/share/elasticsearch/config/elasticsearch.yml

RUN chown -R elasticsearch:elasticsearch /usr/share/elasticsearch/data/quantexa

EXPOSE 9200 9300
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["elasticsearch"]