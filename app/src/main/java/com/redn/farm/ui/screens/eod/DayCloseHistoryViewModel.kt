package com.redn.farm.ui.screens.eod

import androidx.lifecycle.ViewModel
import com.redn.farm.data.local.entity.DayCloseEntity
import com.redn.farm.data.repository.DayCloseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class DayCloseHistoryViewModel @Inject constructor(
    repo: DayCloseRepository,
) : ViewModel() {
    val closes: Flow<List<DayCloseEntity>> = repo.getFinalizedDesc()
}
