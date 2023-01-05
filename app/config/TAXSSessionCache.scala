/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import com.google.inject.Inject
import com.google.inject.name.Named
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.cache.client.SessionCache

class TAXSSessionCache @Inject() (val http: HttpClient, @Named("appName") appName: String)(implicit
  val appConfig: ApplicationConfig
) extends SessionCache {
  override lazy val defaultSource: String = appName
  override lazy val baseUri: String       = appConfig.sessionCacheHost
  override lazy val domain: String        = appConfig.sessionCacheDomain
}
