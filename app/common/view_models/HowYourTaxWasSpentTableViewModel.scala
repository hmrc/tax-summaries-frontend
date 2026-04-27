package common.view_models

import uk.gov.hmrc.govukfrontend.views.Aliases.{HeadCell, TableRow}

final case class HowYourTaxWasSpentTableViewModel(
   caption: String,
   head: Seq[HeadCell],
   rows: Seq[Seq[TableRow]]
 )