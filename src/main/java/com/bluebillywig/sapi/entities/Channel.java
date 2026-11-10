package com.bluebillywig.sapi.entities;

import com.bluebillywig.sapi.Entity;
import com.bluebillywig.sapi.RequestOptions;
import com.bluebillywig.sapi.SapiResponse;
import com.bluebillywig.sapi.Sdk;
import com.bluebillywig.sapi.contracts.Creatable;
import com.bluebillywig.sapi.contracts.Deletable;
import com.bluebillywig.sapi.contracts.Gettable;
import com.bluebillywig.sapi.contracts.Listable;
import com.bluebillywig.sapi.contracts.Updatable;
import com.bluebillywig.sapi.types.ChannelProps;
import com.bluebillywig.sapi.util.QueryParams;

/** CRUD operations on {@code /sapi/channel}. */
public final class Channel extends Entity
        implements Listable, Gettable, Creatable<ChannelProps>, Updatable<ChannelProps>, Deletable {
    private static final String PATH = "/sapi/channel";

    /**
     * Creates the entity.
     *
     * @param sdk the SDK
     */
    public Channel(Sdk sdk) {
        super(sdk);
    }

    @Override
    public SapiResponse list(int limit, int offset, String sort) {
        return getSdk().sendRequest("GET", PATH, new RequestOptions().setQuery(QueryParams.build(
                QueryParams.param("limit", limit),
                QueryParams.param("offset", offset),
                QueryParams.param("sort", sort))));
    }

    @Override
    public SapiResponse get(String id) {
        return getSdk().sendRequest("GET", PATH + "/" + QueryParams.encode(id));
    }

    @Override
    public SapiResponse create(ChannelProps props) {
        return getSdk().sendRequest("PUT", PATH, new RequestOptions().setJson(props));
    }

    @Override
    public SapiResponse update(String id, ChannelProps props) {
        return getSdk().sendRequest("PUT", PATH + "/" + QueryParams.encode(id), new RequestOptions().setJson(props));
    }

    @Override
    public SapiResponse delete(String id) {
        return getSdk().sendRequest("DELETE", PATH + "/" + QueryParams.encode(id));
    }
}
