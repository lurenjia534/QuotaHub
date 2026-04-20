package com.lurenjia534.quotahub.data.provider.zhipu

import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjector
import com.lurenjia534.quotahub.data.provider.monitor.MonitorQuotaSubscriptionCardProjector

class ZhipuSubscriptionCardProjector : SubscriptionCardProjector by MonitorQuotaSubscriptionCardProjector()
