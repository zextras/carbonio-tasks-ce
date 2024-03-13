// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.dal.repositories.DbInfoRepository;
import java.io.FileNotFoundException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface DatabaseInitializer {

  void initialize();
}
